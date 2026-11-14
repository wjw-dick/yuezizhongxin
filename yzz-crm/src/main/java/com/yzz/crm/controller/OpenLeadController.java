package com.yzz.crm.controller;

import com.yzz.common.api.R;
import com.yzz.common.guard.Idempotent;
import com.yzz.crm.entity.CrmLead;
import com.yzz.crm.entity.CrmVisit;
import com.yzz.crm.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 官网/小程序/H5 留资与参观预约（T59、T73、T87）。
 *
 * <p>留资入线索池并带渠道归因；同一手机号 24 小时内重复提交只更新预产期与意向，
 * 不重复创建线索。返回体不暴露线索主键以外的任何内部字段。</p>
 */
@Tag(name = "开放接口-留资预约")
@RestController
@RequestMapping("/api/open/leads")
public class OpenLeadController {

    private final LeadService leadService;

    public OpenLeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @Operation(summary = "官网/小程序留资")
    @Idempotent(ttlSeconds = 30)
    @PostMapping
    public R<Map<String, Object>> submit(@RequestBody CrmLead request) {
        Long id = leadService.create(request);
        return R.ok(Map.of("leadId", id, "message", "已收到您的信息，顾问将在 30 分钟内联系您"));
    }

    @Operation(summary = "预约参观")
    @PostMapping("/visits")
    public R<Map<String, Object>> visit(@RequestBody CrmVisit request) {
        Long id = leadService.createVisit(request);
        return R.ok(Map.of("visitId", id, "message", "预约成功，到店请出示预约信息"));
    }
}
