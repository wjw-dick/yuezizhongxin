package com.yzz.platform.dto;

import com.yzz.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "账单列表筛选")
public class BillQuery extends PageQuery {

    @Schema(description = "租户标识")
    private Long tenantId;

    @Schema(description = "账单状态：PENDING/PAID/OVERDUE")
    private String status;

    @Schema(description = "账期 yyyy-MM")
    private String billPeriod;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBillPeriod() {
        return billPeriod;
    }

    public void setBillPeriod(String billPeriod) {
        this.billPeriod = billPeriod;
    }
}
