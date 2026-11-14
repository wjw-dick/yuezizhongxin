package com.yzz.tenant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户初始化模板：默认角色、默认菜单、默认系统参数、角色菜单授权。
 *
 * <p>开通链路要求 ≤3 分钟，本实现为单事务批量写入，实测为秒级。
 * 对象存储目录按 {@code tenants/{租户标识}/} 约定在首次上传时自动创建；
 * 平台级统一字典由平台统一下发，租户侧不再复刻一份，保证「中文名称唯一出口」。</p>
 */
@Service
public class TenantInitService {

    private static final Logger log = LoggerFactory.getLogger(TenantInitService.class);

    private static final List<String[]> ROLES = List.of(
            new String[]{"tenant_admin", "租户管理员"},
            new String[]{"store_manager", "店长"},
            new String[]{"sales", "销售顾问"},
            new String[]{"care_supervisor", "护理主管"},
            new String[]{"nurse", "护理师"},
            new String[]{"nutritionist", "营养师"},
            new String[]{"finance", "财务"},
            new String[]{"service", "客服专员"},
            new String[]{"content", "内容运营"});

    private static final List<String[]> MENUS = List.of(
            new String[]{"工作台", "/tenant/workbench", "workbench"},
            new String[]{"房态与档期", "/tenant/room", "room"},
            new String[]{"套餐与商品", "/tenant/product", "product"},
            new String[]{"线索CRM", "/tenant/crm", "crm"},
            new String[]{"订单管理", "/tenant/order", "order"},
            new String[]{"入馆与出馆", "/tenant/stay", "stay"},
            new String[]{"护理服务", "/tenant/care", "care"},
            new String[]{"产后康复", "/tenant/rehab", "rehab"},
            new String[]{"月子餐", "/tenant/meal", "meal"},
            new String[]{"排班与人力", "/tenant/staff", "staff"},
            new String[]{"会员营销", "/tenant/member", "member"},
            new String[]{"内容CMS", "/tenant/content", "content"},
            new String[]{"报表分析", "/tenant/report", "report"},
            new String[]{"系统配置", "/tenant/system", "system"});

    private static final List<String[]> CONFIGS = List.of(
            new String[]{"订单规则", "order.deposit.ratio", "30", "number", "定金比例（%）"},
            new String[]{"订单规则", "order.tail.payment.days", "3", "number", "尾款支付提前天数"},
            new String[]{"订单规则", "order.cancel.penalty.ratio", "20", "number", "退订违约金比例（%）"},
            new String[]{"订单规则", "order.schedule.hold.minutes", "30", "number", "销售预留档期保留时长（分钟）"},
            new String[]{"订单规则", "order.deposit.amount", "5000", "number", "押金金额（元）"},
            new String[]{"护理规则", "care.ratio.max", "1", "number", "一对一护理配比上限"},
            new String[]{"护理规则", "care.warning.temperature", "37.5", "number", "体温预警阈值（℃）"},
            new String[]{"营销规则", "marketing.point.ratio", "1", "number", "消费积分比例（元/分）"},
            new String[]{"站点基础", "site.name", "月子中心", "string", "站点名称"},
            new String[]{"站点基础", "site.icp", "", "string", "ICP 备案号"},
            new String[]{"多语言", "i18n.default", "zh-CN", "string", "默认语言"},
            new String[]{"消息", "sms.enabled", "false", "boolean", "短信渠道是否启用"});

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTenantMapper tenantMapper;

    public TenantInitService(JdbcTemplate jdbcTemplate, PlatformTenantMapper tenantMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantMapper = tenantMapper;
    }

    /**
     * 初始化租户默认数据。重复调用安全：角色已存在则跳过，避免重复开通写脏数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void init(PlatformTenant tenant) {
        long start = System.currentTimeMillis();
        Long tenantId = tenant.getId();

        Integer exists = jdbcTemplate.queryForObject(
                "select count(1) from sys_role where tenant_id = ?", Integer.class, tenantId);
        if (exists != null && exists > 0) {
            log.info("租户 {} 已初始化，跳过", tenant.getTenantCode());
            return;
        }

        List<Object[]> roleRows = new ArrayList<>();
        for (String[] role : ROLES) {
            roleRows.add(new Object[]{tenantId, role[0], role[1], "开通默认角色"});
        }
        jdbcTemplate.batchUpdate(
                "insert into sys_role(tenant_id, role_code, role_name, remark) values(?,?,?,?)", roleRows);

        List<Object[]> menuRows = new ArrayList<>();
        for (int i = 0; i < MENUS.size(); i++) {
            String[] menu = MENUS.get(i);
            menuRows.add(new Object[]{tenantId, menu[0], menu[1], menu[2], (i + 1) * 10});
        }
        jdbcTemplate.batchUpdate(
                "insert into sys_menu(tenant_id, title, path, perm, sort) values(?,?,?,?,?)", menuRows);

        List<Object[]> configRows = new ArrayList<>();
        for (String[] config : CONFIGS) {
            configRows.add(new Object[]{tenantId, config[0], config[1], config[2], config[3], config[4]});
        }
        jdbcTemplate.batchUpdate(
                "insert into sys_config(tenant_id, config_group, config_key, config_value, value_type, remark) "
                        + "values(?,?,?,?,?,?)", configRows);

        // 租户管理员默认拥有全部菜单
        jdbcTemplate.update("insert into sys_role_menu(tenant_id, role_id, menu_id) "
                + "select m.tenant_id, r.id, m.id from sys_menu m join sys_role r "
                + "on r.tenant_id = m.tenant_id and r.role_code = 'tenant_admin' where m.tenant_id = ?", tenantId);

        // 租户状态写入用量表初始行，便于平台看板与配额校验
        jdbcTemplate.update("insert into platform_tenant_usage(tenant_id, used_seat, used_mp, used_storage_gb, "
                + "used_sms, used_api, stat_date) values(?,0,0,0,0,0,curdate())", tenantId);

        log.info("租户 {} 初始化完成，耗时 {} ms", tenant.getTenantCode(), System.currentTimeMillis() - start);
    }

    /** 主管理员账号创建成功后回填用量表席位。 */
    @Transactional(rollbackFor = Exception.class)
    public void refreshSeatUsage(Long tenantId) {
        Integer seats = jdbcTemplate.queryForObject(
                "select count(1) from sys_user where tenant_id = ? and status = 'ENABLED'", Integer.class, tenantId);
        jdbcTemplate.update("update platform_tenant_usage set used_seat = ?, stat_date = curdate() "
                + "where tenant_id = ?", seats == null ? 0 : seats, tenantId);
    }

    public boolean exists(String tenantCode) {
        Long count = tenantMapper.selectCount(Wrappers.<PlatformTenant>lambdaQuery()
                .eq(PlatformTenant::getTenantCode, tenantCode));
        return count != null && count > 0;
    }
}
