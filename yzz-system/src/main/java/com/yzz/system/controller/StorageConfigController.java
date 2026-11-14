package com.yzz.system.controller;

import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.system.dto.StorageConfigRequest;
import com.yzz.system.dto.StorageTestRequest;
import com.yzz.system.service.StorageConfigService;
import com.yzz.system.service.TenantStorageRouter;
import com.yzz.system.vo.StorageConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对象存储配置（T23）：连接测试通过才可保存，切换驱动需二次确认。
 */
@Tag(name = "租户-对象存储")
@RestController
@RequestMapping("/api/tenant/storage-config")
public class StorageConfigController {

    private final StorageConfigService configService;
    private final TenantStorageRouter storageRouter;

    public StorageConfigController(StorageConfigService configService,
                                   TenantStorageRouter storageRouter) {
        this.configService = configService;
        this.storageRouter = storageRouter;
    }

    @Operation(summary = "支持的驱动")
    @GetMapping("/drivers")
    public R<List<String>> drivers() {
        AuthGuard.requireTenant();
        return R.ok(StorageConfigService.DRIVERS);
    }

    @Operation(summary = "当前配置（密钥掩码）")
    @GetMapping
    public R<StorageConfigVO> current() {
        AuthGuard.requireTenant();
        return R.ok(configService.current());
    }

    @Operation(summary = "连接测试")
    @PostMapping("/test")
    public R<String> test(@Valid @RequestBody StorageTestRequest request) {
        AuthGuard.requireTenant();
        return R.ok(configService.test(request));
    }

    @Operation(summary = "保存配置")
    @AuditLog(module = "对象存储", action = "保存配置", withArgs = false)
    @PutMapping
    public R<StorageConfigVO> save(@Valid @RequestBody StorageConfigRequest request) {
        AuthGuard.requireTenant();
        StorageConfigVO vo = configService.save(request);
        // 立即失效路由缓存，切换驱动无需等待一分钟
        storageRouter.evict(TenantContext.getTenantId());
        return R.ok(vo);
    }
}
