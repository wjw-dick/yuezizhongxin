package com.yzz.report.service;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.entity.SysExportLog;
import com.yzz.system.mapper.SysExportLogMapper;
import com.yzz.system.service.ConfigService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 导出管控（T52）。
 *
 * <p>四条硬规则：用途必填、单次上限 5000 行、内容带水印、每次导出写留痕。
 * 若系统参数 `export.approval.enabled=true`，还必须带审批通过的标记。
 * 产物为 Excel 可直接打开的 SpreadsheetML/HTML 表格，无需额外依赖。</p>
 */
@Service
public class ExportService {

    private static final int MAX_ROWS = 5000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SysExportLogMapper exportLogMapper;
    private final ConfigService configService;

    public ExportService(SysExportLogMapper exportLogMapper, ConfigService configService) {
        this.exportLogMapper = exportLogMapper;
        this.configService = configService;
    }

    public ExportFile excel(String module, String purpose, boolean approved,
                            List<String> headers, List<List<Object>> rows) {
        if (purpose == null || purpose.isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "导出用途必填");
        }
        if (rows.size() > MAX_ROWS) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST,
                    "单次导出最多 " + MAX_ROWS + " 行，请缩小筛选范围后分批导出");
        }
        if (configService.getBoolean("export.approval.enabled", false) && !approved) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "导出审批未通过，请联系管理员审批后再导出");
        }
        String watermark = watermark();
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"/>")
                .append("<style>table{border-collapse:collapse}td,th{border:1px solid #999;")
                .append("padding:4px 8px;font-size:12px}th{background:#eef6f2}</style></head><body>");
        html.append("<div style=\"color:#888;font-size:11px\">").append(escape(watermark)).append("</div>");
        html.append("<table><thead><tr>");
        for (String header : headers) {
            html.append("<th>").append(escape(header)).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (List<Object> row : rows) {
            html.append("<tr>");
            for (Object cell : row) {
                html.append("<td>").append(escape(cell == null ? "" : String.valueOf(cell))).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<div style=\"color:#bbb;font-size:10px;margin-top:8px\">")
                .append(escape(watermark)).append("</div></body></html>");

        SysExportLog log = new SysExportLog();
        log.setTenantId(TenantContext.requireTenantId());
        log.setModule(module);
        log.setPurpose(purpose);
        log.setRowCount(rows.size());
        log.setWatermark(watermark);
        log.setOperatorId(TenantContext.getUserId());
        exportLogMapper.insert(log);

        String filename = module + "-" + LocalDateTime.now().format(TS) + ".xls";
        return new ExportFile(html.toString().getBytes(StandardCharsets.UTF_8), filename,
                "application/vnd.ms-excel;charset=UTF-8", rows.size());
    }

    private String watermark() {
        String tenant = TenantContext.getTenantCode() == null ? "-" : TenantContext.getTenantCode();
        String user = TenantContext.getUsername() == null ? "-" : TenantContext.getUsername();
        return "月之孕 · 租户 " + tenant + " · 导出人 " + user + " · "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " · 仅限内部使用";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 导出结果载体。 */
    public record ExportFile(byte[] content, String filename, String contentType, int rowCount) {
    }
}
