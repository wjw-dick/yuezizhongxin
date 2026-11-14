package com.yzz.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.PageResult;
import com.yzz.common.audit.AuditEvent;
import com.yzz.common.audit.AuditRecorder;
import com.yzz.common.tenant.TenantState;
import com.yzz.platform.dto.QuotaAdjustRequest;
import com.yzz.platform.dto.TenantCreateRequest;
import com.yzz.platform.dto.TenantQuery;
import com.yzz.platform.vo.TenantCreateResult;
import com.yzz.platform.vo.TenantVO;
import com.yzz.tenant.entity.PlatformPackage;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.entity.PlatformTenantUsage;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import com.yzz.tenant.mapper.PlatformTenantUsageMapper;
import com.yzz.tenant.service.TenantInitService;
import com.yzz.tenant.service.TenantStatusProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 租户管理：开通、停用、恢复、续期、删除、配额调整。
 *
 * <p>开通链路：写租户 → 初始化模板（角色/菜单/参数） → 生成主管理员 → 回填用量，
 * 单事务完成，目标 ≤3 分钟（实际秒级）。</p>
 */
@Service
public class PlatformTenantService {

    private static final Logger log = LoggerFactory.getLogger(PlatformTenantService.class);
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{1,30}$");
    private static final String PASSWORD_CHARS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PlatformTenantMapper tenantMapper;
    private final PlatformTenantUsageMapper usageMapper;
    private final PlatformPackageService packageService;
    private final TenantInitService initService;
    private final TenantStatusProviderImpl statusProvider;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<AuditRecorder> auditRecorder;
    private final SecureRandom random = new SecureRandom();

    public PlatformTenantService(PlatformTenantMapper tenantMapper,
                                 PlatformTenantUsageMapper usageMapper,
                                 PlatformPackageService packageService,
                                 TenantInitService initService,
                                 TenantStatusProviderImpl statusProvider,
                                 PasswordEncoder passwordEncoder,
                                 JdbcTemplate jdbcTemplate,
                                 ObjectProvider<AuditRecorder> auditRecorder) {
        this.tenantMapper = tenantMapper;
        this.usageMapper = usageMapper;
        this.packageService = packageService;
        this.initService = initService;
        this.statusProvider = statusProvider;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.auditRecorder = auditRecorder;
    }

    public PageResult<TenantVO> page(TenantQuery query) {
        var wrapper = Wrappers.<PlatformTenant>lambdaQuery()
                .like(query.getTenantCode() != null && !query.getTenantCode().isBlank(),
                        PlatformTenant::getTenantCode, query.getTenantCode())
                .like(query.getTenantName() != null && !query.getTenantName().isBlank(),
                        PlatformTenant::getTenantName, query.getTenantName())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                        PlatformTenant::getStatus, query.getStatus())
                .orderByDesc(PlatformTenant::getCreatedTime);
        if (Boolean.TRUE.equals(query.getExpiringSoon())) {
            wrapper.le(PlatformTenant::getExpireTime, LocalDateTime.now().plusDays(30))
                    .isNotNull(PlatformTenant::getExpireTime);
        }
        IPage<PlatformTenant> page = tenantMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<TenantVO> records = new ArrayList<>();
        for (PlatformTenant tenant : page.getRecords()) {
            records.add(toVO(tenant));
        }
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public TenantVO detail(Long id) {
        return toVO(require(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantCreateResult create(TenantCreateRequest request) {
        String code = request.tenantCode().trim().toLowerCase();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST,
                    "租户标识仅支持小写字母、数字与中划线，且以字母开头，长度 2~31");
        }
        if (initService.exists(code)) {
            throw BusinessException.of(ErrorCode.CONFLICT, "租户标识已存在");
        }
        PlatformPackage pkg = packageService.require(request.packageId());

        LocalDateTime now = LocalDateTime.now();
        int trialDays = request.trialDays() == null ? 0 : request.trialDays();
        PlatformTenant tenant = new PlatformTenant();
        tenant.setTenantCode(code);
        tenant.setTenantName(request.tenantName().trim());
        tenant.setIndustry(request.industry());
        tenant.setContactName(request.contactName());
        tenant.setContactPhone(request.contactPhone());
        tenant.setPackageId(pkg.getId());
        tenant.setSeatLimit(pkg.getSeatQuota());
        tenant.setMpLimit(pkg.getMpQuota());
        tenant.setStorageLimitGb(pkg.getStorageQuotaGb());
        tenant.setSmsLimit(pkg.getSmsQuota());
        tenant.setApiLimit(pkg.getApiQuota());
        tenant.setCreatedTime(now);
        tenant.setUpdatedTime(now);
        tenant.setDeleted(0);
        if (trialDays > 0) {
            tenant.setStatus(TenantState.TRIAL.name());
            tenant.setTrialEndTime(now.plusDays(trialDays));
            tenant.setExpireTime(now.plusDays(trialDays));
        } else {
            tenant.setStatus(TenantState.NORMAL.name());
            tenant.setExpireTime("年".equals(pkg.getBillingCycle()) ? now.plusDays(365) : now.plusDays(30));
        }
        tenantMapper.insert(tenant);

        initService.init(tenant);

        String initialPassword = randomPassword();
        int inserted = jdbcTemplate.update(
                "insert into sys_user(tenant_id, username, password, real_name, role_id, status) "
                        + "select ?, ?, ?, ?, r.id, 'ENABLED' from sys_role r "
                        + "where r.tenant_id = ? and r.role_code = 'tenant_admin'",
                tenant.getId(), request.adminUsername().trim(),
                passwordEncoder.encode(initialPassword),
                request.adminRealName() == null ? request.adminUsername() : request.adminRealName(),
                tenant.getId());
        if (inserted == 0) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "主管理员创建失败，请重试");
        }
        initService.refreshSeatUsage(tenant.getId());
        statusProvider.evict(code);
        log.info("租户开通完成 code={} tenantId={}", code, tenant.getId());
        return new TenantCreateResult(tenant.getId(), code,
                request.adminUsername().trim(), initialPassword);
    }

    public void disable(Long id) {
        PlatformTenant tenant = require(id);
        updateStatus(tenant, TenantState.DISABLED);
        // 停用即清理该租户状态缓存，配合状态过滤器实现「停用即拒登录」
        statusProvider.evict(tenant.getTenantCode());
    }

    public void resume(Long id) {
        PlatformTenant tenant = require(id);
        boolean expired = tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now());
        updateStatus(tenant, expired ? TenantState.EXPIRED : TenantState.NORMAL);
        statusProvider.evict(tenant.getTenantCode());
        if (expired) {
            throw BusinessException.of(ErrorCode.TENANT_EXPIRED, "该租户服务已到期，请先续期再恢复");
        }
    }

    public void renew(Long id, int days) {
        PlatformTenant tenant = require(id);
        LocalDateTime base = tenant.getExpireTime() == null || tenant.getExpireTime().isBefore(LocalDateTime.now())
                ? LocalDateTime.now()
                : tenant.getExpireTime();
        PlatformTenant update = new PlatformTenant();
        update.setId(tenant.getId());
        update.setExpireTime(base.plusDays(days));
        update.setStatus(TenantState.NORMAL.name());
        update.setUpdatedTime(LocalDateTime.now());
        tenantMapper.updateById(update);
        statusProvider.evict(tenant.getTenantCode());
    }

    /**
     * 删除租户：先置为停用再逻辑删除，业务数据保留并归档提示，不做物理删除。
     */
    public void remove(Long id) {
        PlatformTenant tenant = require(id);
        updateStatus(tenant, TenantState.DISABLED);
        tenantMapper.deleteById(id);
        statusProvider.evict(tenant.getTenantCode());
    }

    public void adjustQuota(Long id, QuotaAdjustRequest request) {
        PlatformTenant tenant = require(id);
        PlatformTenant update = new PlatformTenant();
        update.setId(id);
        update.setSeatLimit(request.seatLimit());
        update.setMpLimit(request.mpLimit());
        update.setStorageLimitGb(request.storageLimitGb());
        update.setSmsLimit(request.smsLimit());
        update.setApiLimit(request.apiLimit());
        update.setUpdatedTime(LocalDateTime.now());
        tenantMapper.updateById(update);

        AuditRecorder recorder = auditRecorder.getIfAvailable();
        if (recorder != null) {
            recorder.record(new AuditEvent("租户管理", "调整配额", "platform_tenant:" + id,
                    request.reason(),
                    true, null, tenant.getId(), tenant.getTenantCode(), true,
                    null, "平台操作", "", LocalDateTime.now()));
        }
    }

    public PlatformTenant require(Long id) {
        PlatformTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "租户不存在或已删除");
        }
        return tenant;
    }

    private void updateStatus(PlatformTenant tenant, TenantState state) {
        PlatformTenant update = new PlatformTenant();
        update.setId(tenant.getId());
        update.setStatus(state.name());
        update.setUpdatedTime(LocalDateTime.now());
        tenantMapper.updateById(update);
    }

    private TenantVO toVO(PlatformTenant tenant) {
        PlatformTenantUsage usage = usageMapper.selectOne(Wrappers.<PlatformTenantUsage>lambdaQuery()
                .eq(PlatformTenantUsage::getTenantId, tenant.getId())
                .orderByDesc(PlatformTenantUsage::getStatDate)
                .last("limit 1"));
        PlatformPackage pkg = tenant.getPackageId() == null ? null : packageService.require(tenant.getPackageId());
        TenantState state = TenantState.of(tenant.getStatus());
        return new TenantVO(
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getTenantName(),
                tenant.getIndustry(),
                tenant.getContactName(),
                tenant.getContactPhone(),
                tenant.getPackageId(),
                pkg == null ? "" : pkg.getPackageName(),
                tenant.getStatus(),
                state.getLabel(),
                tenant.getTrialEndTime(),
                tenant.getExpireTime(),
                tenant.getSeatLimit(),
                usage == null ? 0 : usage.getUsedSeat(),
                tenant.getMpLimit(),
                usage == null ? 0 : usage.getUsedMp(),
                tenant.getStorageLimitGb(),
                usage == null ? 0 : usage.getUsedStorageGb(),
                tenant.getSmsLimit(),
                usage == null ? 0 : usage.getUsedSms(),
                tenant.getApiLimit(),
                usage == null ? 0 : usage.getUsedApi(),
                tenant.getCreatedTime());
    }

    private String randomPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
