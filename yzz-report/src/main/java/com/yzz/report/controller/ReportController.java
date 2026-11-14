package com.yzz.report.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.mapper.BizOrderMapper;
import com.yzz.report.service.ExportService;
import com.yzz.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报表查询与导出（T49、T52）。
 */
@Tag(name = "租户-报表")
@RestController
@RequestMapping("/api/tenant/reports")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;
    private final BizOrderMapper orderMapper;

    public ReportController(ReportService reportService, ExportService exportService,
                            BizOrderMapper orderMapper) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.orderMapper = orderMapper;
    }

    @Operation(summary = "工作台聚合（今日概览 + 提醒）")
    @GetMapping("/workbench")
    public R<Map<String, Object>> workbench() {
        AuthGuard.requireTenant();
        return R.ok(reportService.workbench());
    }

    @Operation(summary = "经营日报")
    @GetMapping("/daily")
    public R<Map<String, Object>> daily(@RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(reportService.daily(date));
    }

    @Operation(summary = "经营月报")
    @GetMapping("/monthly")
    public R<Map<String, Object>> monthly(@RequestParam(required = false) String month) {
        AuthGuard.requireTenant();
        YearMonth target = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        return R.ok(reportService.monthly(target));
    }

    @Operation(summary = "护理报表")
    @GetMapping("/care")
    public R<Map<String, Object>> care(@RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        return R.ok(reportService.care(from, to));
    }

    @Operation(summary = "人力报表")
    @GetMapping("/staff")
    public R<List<Map<String, Object>>> staff(@RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        return R.ok(reportService.staff(from, to));
    }

    @Operation(summary = "餐食报表")
    @GetMapping("/meal")
    public R<Map<String, Object>> meal(@RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuthGuard.requireTenant();
        return R.ok(reportService.meal(from, to));
    }

    /**
     * 导出：module 支持 orders / care / staff / meal。
     */
    @Operation(summary = "导出 Excel（带水印与留痕）")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        String module = String.valueOf(body.getOrDefault("module", "orders"));
        String purpose = (String) body.get("purpose");
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", "false")));
        ExportService.ExportFile file = switch (module) {
            case "orders" -> exportOrders(purpose, approved);
            case "care" -> exportGeneric("护理报表", purpose, approved,
                    List.of("指标", "数值"),
                    toRows(reportService.care(null, null)));
            case "staff" -> exportGeneric("人力报表", purpose, approved,
                    List.of("姓名", "岗位", "班次数", "出勤天数"),
                    reportService.staff(null, null).stream()
                            .map(m -> List.of(nullSafe(m.get("name")), nullSafe(m.get("jobType")),
                                    nullSafe(m.get("shiftCount")), nullSafe(m.get("attendanceDays"))))
                            .toList());
            case "meal" -> exportGeneric("餐食报表", purpose, approved,
                    List.of("指标", "数值"), toRows(reportService.meal(null, null)));
            default -> throw new IllegalArgumentException("不支持的导出模块：" + module);
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDispositionFormData("attachment", file.filename());
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(file.filename(),
                        java.nio.charset.StandardCharsets.UTF_8));
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    private ExportService.ExportFile exportOrders(String purpose, boolean approved) {
        List<BizOrder> rows = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .orderByDesc(BizOrder::getId).last("limit 5000"));
        List<List<Object>> data = new ArrayList<>();
        for (BizOrder order : rows) {
            data.add(List.of(order.getOrderNo(), order.getCustomerName(), order.getStatus(),
                    order.getCheckinDate() == null ? "" : order.getCheckinDate().toString(),
                    order.getCheckoutDate() == null ? "" : order.getCheckoutDate().toString(),
                    order.getTotalAmount(), order.getPaidAmount(), order.getSource() == null ? "" : order.getSource()));
        }
        return exportService.excel("订单明细", purpose, approved,
                List.of("订单号", "客户", "状态", "入住日期", "出馆日期", "应收", "已收", "来源"), data);
    }

    private ExportService.ExportFile exportGeneric(String module, String purpose, boolean approved,
                                                   List<String> headers, List<List<Object>> rows) {
        return exportService.excel(module, purpose, approved, headers, rows);
    }

    private List<List<Object>> toRows(Map<String, Object> map) {
        List<List<Object>> rows = new ArrayList<>();
        map.forEach((key, value) -> {
            if (value instanceof Map || value instanceof List) {
                return;
            }
            rows.add(List.of(key, value == null ? "" : String.valueOf(value)));
        });
        return rows;
    }

    private Object nullSafe(Object value) {
        return value == null ? "" : value;
    }
}
