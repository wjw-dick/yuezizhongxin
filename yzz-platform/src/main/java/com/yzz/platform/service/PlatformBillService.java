package com.yzz.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.PageResult;
import com.yzz.platform.dto.BillQuery;
import com.yzz.tenant.entity.PlatformBill;
import com.yzz.tenant.entity.PlatformPackage;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.entity.PlatformTenantUsage;
import com.yzz.tenant.mapper.PlatformBillMapper;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import com.yzz.tenant.mapper.PlatformTenantUsageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单与计费：按席位用量生成账单，状态流转 待支付 → 已支付 / 已逾期。
 *
 * <p>计费口径：套餐定额 + 超出席位按量补差（每席位 200 元/账期，作为参数常量，
 * 正式上线前应迁移到系统参数）。</p>
 */
@Service
public class PlatformBillService {

    private static final BigDecimal OVERAGE_UNIT_PRICE = new BigDecimal("200");
    private static final String PENDING = "PENDING";
    private static final String PAID = "PAID";

    private final PlatformBillMapper billMapper;
    private final PlatformTenantMapper tenantMapper;
    private final PlatformTenantUsageMapper usageMapper;
    private final PlatformPackageService packageService;

    public PlatformBillService(PlatformBillMapper billMapper, PlatformTenantMapper tenantMapper,
                               PlatformTenantUsageMapper usageMapper,
                               PlatformPackageService packageService) {
        this.billMapper = billMapper;
        this.tenantMapper = tenantMapper;
        this.usageMapper = usageMapper;
        this.packageService = packageService;
    }

    public PageResult<PlatformBill> page(BillQuery query) {
        var wrapper = Wrappers.<PlatformBill>lambdaQuery()
                .eq(query.getTenantId() != null, PlatformBill::getTenantId, query.getTenantId())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                        PlatformBill::getStatus, query.getStatus())
                .eq(query.getBillPeriod() != null && !query.getBillPeriod().isBlank(),
                        PlatformBill::getBillPeriod, query.getBillPeriod())
                .orderByDesc(PlatformBill::getCreatedTime);
        IPage<PlatformBill> page = billMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 按账期批量生成账单。同一租户同一账期已存在账单则跳过，保证可重复执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generate(String billPeriod) {
        List<PlatformTenant> tenants = tenantMapper.selectList(Wrappers.<PlatformTenant>lambdaQuery()
                .in(PlatformTenant::getStatus, "TRIAL", "NORMAL", "ARREARS"));
        int created = 0;
        for (PlatformTenant tenant : tenants) {
            Long exists = billMapper.selectCount(Wrappers.<PlatformBill>lambdaQuery()
                    .eq(PlatformBill::getTenantId, tenant.getId())
                    .eq(PlatformBill::getBillPeriod, billPeriod));
            if (exists != null && exists > 0) {
                continue;
            }
            PlatformPackage pkg = packageService.require(tenant.getPackageId());
            PlatformTenantUsage usage = usageMapper.selectOne(Wrappers.<PlatformTenantUsage>lambdaQuery()
                    .eq(PlatformTenantUsage::getTenantId, tenant.getId())
                    .orderByDesc(PlatformTenantUsage::getStatDate)
                    .last("limit 1"));
            int seats = usage == null || usage.getUsedSeat() == null ? 0 : usage.getUsedSeat();
            int overage = Math.max(0, seats - (tenant.getSeatLimit() == null ? 0 : tenant.getSeatLimit()));
            BigDecimal amount = pkg.getPrice()
                    .add(OVERAGE_UNIT_PRICE.multiply(BigDecimal.valueOf(overage)));

            PlatformBill bill = new PlatformBill();
            bill.setTenantId(tenant.getId());
            bill.setBillPeriod(billPeriod);
            bill.setItemName("套餐费（" + pkg.getPackageName() + "）"
                    + (overage > 0 ? " + 超出席位 " + overage + " 个" : ""));
            bill.setQuantity(seats);
            bill.setAmount(amount);
            bill.setStatus(PENDING);
            bill.setCreatedTime(LocalDateTime.now());
            billMapper.insert(bill);
            created++;
        }
        return created;
    }

    public void markPaid(Long id) {
        PlatformBill bill = billMapper.selectById(id);
        if (bill == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "账单不存在");
        }
        if (PAID.equals(bill.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该账单已标记为已支付");
        }
        PlatformBill update = new PlatformBill();
        update.setId(id);
        update.setStatus(PAID);
        update.setPayTime(LocalDateTime.now());
        billMapper.updateById(update);
    }

    /** 逾期扫描：待支付且创建超过 15 天的账单置为已逾期，并可由平台侧触发租户欠费状态。 */
    @Transactional(rollbackFor = Exception.class)
    public int markOverdue() {
        List<PlatformBill> overdue = billMapper.selectList(Wrappers.<PlatformBill>lambdaQuery()
                .eq(PlatformBill::getStatus, PENDING)
                .lt(PlatformBill::getCreatedTime, LocalDateTime.now().minusDays(15)));
        for (PlatformBill bill : overdue) {
            PlatformBill update = new PlatformBill();
            update.setId(bill.getId());
            update.setStatus("OVERDUE");
            billMapper.updateById(update);
        }
        return overdue.size();
    }
}
