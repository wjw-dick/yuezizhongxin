package com.yzz.common.tenant;

/**
 * 租户状态提供者。由 yzz-tenant 模块基于平台租户表实现，公共层只依赖抽象，
 * 保证依赖方向为 platform → tenant → common。
 */
public interface TenantStatusProvider {

    /**
     * 查询租户当前状态。租户不存在时返回 {@link TenantState#UNKNOWN}。
     *
     * @param tenantCode 租户编码
     */
    TenantState stateOf(String tenantCode);

    /**
     * 租户主键。开放接口（官网/小程序/H5）无登录态，需要按租户编码回填上下文，
     * 由实现类查询平台租户表；未实现或租户不存在时返回 null。
     */
    default Long idOf(String tenantCode) {
        return null;
    }
}
