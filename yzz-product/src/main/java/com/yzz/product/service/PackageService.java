package com.yzz.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.sensitive.SensitiveWordFilter;
import com.yzz.common.tenant.TenantContext;
import com.yzz.product.entity.PkgItem;
import com.yzz.product.entity.PkgPackage;
import com.yzz.product.mapper.PkgItemMapper;
import com.yzz.product.mapper.PkgPackageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 套餐管理（T33）。
 *
 * <p>套餐是订单计价的主体：天数、房型档位、护理模式、康复项目、餐食标准、价格与定金比例。
 * 上架前校验关键字段与敏感词；下架不影响已签约订单。</p>
 */
@Service
public class PackageService {

    private final PkgPackageMapper packageMapper;
    private final PkgItemMapper itemMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    public PackageService(PkgPackageMapper packageMapper, PkgItemMapper itemMapper,
                          SensitiveWordFilter sensitiveWordFilter) {
        this.packageMapper = packageMapper;
        this.itemMapper = itemMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    public List<PkgPackage> list(String status, String keyword) {
        return packageMapper.selectList(Wrappers.<PkgPackage>lambdaQuery()
                .eq(status != null && !status.isBlank(), PkgPackage::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), PkgPackage::getName, keyword)
                .orderByAsc(PkgPackage::getSort).orderByDesc(PkgPackage::getId));
    }

    public PkgPackage detail(Long id) {
        PkgPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        return pkg;
    }

    public List<PkgItem> items(Long packageId) {
        return itemMapper.selectList(Wrappers.<PkgItem>lambdaQuery()
                .eq(PkgItem::getPackageId, packageId).orderByAsc(PkgItem::getId));
    }

    @Transactional
    public Long save(PkgPackage input) {
        validate(input);
        sensitiveWordFilter.assertClean(input.getName() + " " + (input.getDetailHtml() == null ? "" : input.getDetailHtml()));
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getId() == null) {
            if (input.getStatus() == null) {
                input.setStatus("OFF_SHELF");
            }
            packageMapper.insert(input);
        } else {
            PkgPackage exist = detail(input.getId());
            input.setTenantId(exist.getTenantId());
            packageMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        PkgPackage pkg = detail(id);
        if (!List.of("ON_SHELF", "OFF_SHELF").contains(status)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "非法状态：" + status);
        }
        if ("ON_SHELF".equals(status)) {
            validate(pkg);
        }
        pkg.setStatus(status);
        packageMapper.updateById(pkg);
    }

    @Transactional
    public void delete(Long id) {
        detail(id);
        packageMapper.deleteById(id);
    }

    @Transactional
    public void saveItems(Long packageId, List<PkgItem> items) {
        detail(packageId);
        itemMapper.delete(Wrappers.<PkgItem>lambdaQuery().eq(PkgItem::getPackageId, packageId));
        for (PkgItem item : items) {
            PkgItem entity = new PkgItem();
            entity.setTenantId(TenantContext.requireTenantId());
            entity.setPackageId(packageId);
            entity.setItemType(item.getItemType() == null ? "增值服务" : item.getItemType());
            entity.setItemName(item.getItemName());
            entity.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity());
            entity.setPrice(item.getPrice() == null ? BigDecimal.ZERO : item.getPrice());
            itemMapper.insert(entity);
        }
    }

    /** 定金 = 套餐价 × 定金比例，四舍五入到分。 */
    public BigDecimal depositOf(PkgPackage pkg) {
        BigDecimal ratio = pkg.getDepositRatio() == null ? new BigDecimal("30") : pkg.getDepositRatio();
        return pkg.getPrice().multiply(ratio).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void validate(PkgPackage pkg) {
        if (pkg.getName() == null || pkg.getName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "套餐名称不能为空");
        }
        if (pkg.getDays() == null || pkg.getDays() < 1) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "套餐天数至少为 1 天");
        }
        if (pkg.getPrice() == null || pkg.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "套餐价格不能为负");
        }
        if (pkg.getDepositRatio() != null
                && (pkg.getDepositRatio().compareTo(BigDecimal.ZERO) < 0
                || pkg.getDepositRatio().compareTo(new BigDecimal("100")) > 0)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "定金比例需在 0~100 之间");
        }
    }
}
