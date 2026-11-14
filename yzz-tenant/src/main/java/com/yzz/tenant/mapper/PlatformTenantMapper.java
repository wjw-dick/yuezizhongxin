package com.yzz.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzz.tenant.entity.PlatformTenant;

/**
 * 平台租户 Mapper。平台级表不带 tenant_id，由租户插件按表前缀自动跳过。
 */
public interface PlatformTenantMapper extends BaseMapper<PlatformTenant> {
}
