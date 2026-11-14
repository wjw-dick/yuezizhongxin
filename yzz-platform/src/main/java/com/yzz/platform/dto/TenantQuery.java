package com.yzz.platform.dto;

import com.yzz.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 租户列表筛选条件。
 */
@Schema(description = "租户列表筛选")
public class TenantQuery extends PageQuery {

    @Schema(description = "租户编码，模糊匹配")
    private String tenantCode;

    @Schema(description = "租户名称，模糊匹配")
    private String tenantName;

    @Schema(description = "租户状态：TRIAL/NORMAL/ARREARS/DISABLED/EXPIRED")
    private String status;

    @Schema(description = "是否只看 30 天内到期")
    private Boolean expiringSoon;

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getExpiringSoon() {
        return expiringSoon;
    }

    public void setExpiringSoon(Boolean expiringSoon) {
        this.expiringSoon = expiringSoon;
    }
}
