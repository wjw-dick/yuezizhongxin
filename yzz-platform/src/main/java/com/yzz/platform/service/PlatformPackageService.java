package com.yzz.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.platform.dto.PackageSaveRequest;
import com.yzz.tenant.entity.PlatformPackage;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.mapper.PlatformPackageMapper;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 套餐与五类额度配置。
 */
@Service
public class PlatformPackageService {

    private final PlatformPackageMapper packageMapper;
    private final PlatformTenantMapper tenantMapper;

    public PlatformPackageService(PlatformPackageMapper packageMapper, PlatformTenantMapper tenantMapper) {
        this.packageMapper = packageMapper;
        this.tenantMapper = tenantMapper;
    }

    public List<PlatformPackage> list() {
        return packageMapper.selectList(Wrappers.<PlatformPackage>lambdaQuery()
                .orderByAsc(PlatformPackage::getPrice));
    }

    public PlatformPackage save(PackageSaveRequest request) {
        PlatformPackage entity = new PlatformPackage();
        entity.setPackageName(request.packageName());
        entity.setPrice(request.price());
        entity.setBillingCycle(request.billingCycle());
        entity.setEndCount(request.endCount());
        entity.setSeatQuota(request.seatQuota());
        entity.setMpQuota(request.mpQuota());
        entity.setStorageQuotaGb(request.storageQuotaGb());
        entity.setSmsQuota(request.smsQuota());
        entity.setApiQuota(request.apiQuota());
        entity.setTrialSupported(request.trialSupported() == null ? 1 : request.trialSupported());
        entity.setRemark(request.remark());
        if (request.id() == null) {
            entity.setCreatedTime(LocalDateTime.now());
            packageMapper.insert(entity);
        } else {
            entity.setId(request.id());
            packageMapper.updateById(entity);
        }
        return packageMapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        Long used = tenantMapper.selectCount(Wrappers.<PlatformTenant>lambdaQuery()
                .eq(PlatformTenant::getPackageId, id));
        if (used != null && used > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该套餐已被 " + used + " 个租户使用，不可删除");
        }
        packageMapper.deleteById(id);
    }

    public PlatformPackage require(Long packageId) {
        PlatformPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        return pkg;
    }
}
