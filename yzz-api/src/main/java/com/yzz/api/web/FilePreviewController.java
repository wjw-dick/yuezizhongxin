package com.yzz.api.web;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件预览。仅按对象键读取，键必须形如 tenants/{租户标识}/...，其余一律拒绝，
 * 避免通过路径穿越读取其他租户的文件。
 */
@RestController
public class FilePreviewController {

    private static final String PREFIX = "/files/";

    private final StorageService storageService;

    public FilePreviewController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/files/**")
    public ResponseEntity<Resource> preview(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int idx = uri.indexOf(PREFIX);
        if (idx < 0) {
            throw BusinessException.of(ErrorCode.NOT_FOUND);
        }
        String objectKey = uri.substring(idx + PREFIX.length());
        if (objectKey.contains("..")) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED);
        }
        String[] parts = objectKey.split("/");
        if (parts.length < 3 || !"tenants".equals(parts[0])) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED);
        }
        Resource resource = storageService.download(parts[1], objectKey);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "inline")
                .body(resource);
    }
}
