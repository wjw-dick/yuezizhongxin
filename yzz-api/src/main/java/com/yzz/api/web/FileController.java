package com.yzz.api.web;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.R;
import com.yzz.common.storage.StorageService;
import com.yzz.common.storage.StoredFile;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件统一上传入口。所有业务上传（房型图、合同、护理照片、资质文件）都必须走本接口，
 * 由存储门面统一做租户前缀隔离、大小限制与驱动切换。
 */
@Tag(name = "公共-文件上传")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "上传文件（按租户前缀隔离）")
    @PostMapping("/upload")
    public R<StoredFile> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "dir", defaultValue = "common") String dir) {
        AuthGuard.requireLogin();
        if (file.isEmpty()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "上传文件为空");
        }
        try {
            StoredFile stored = storageService.upload(
                    TenantContext.getTenantCode(), dir, file.getOriginalFilename(),
                    file.getInputStream(), file.getSize(), file.getContentType());
            return R.ok(stored);
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "读取上传文件失败");
        }
    }
}
