package com.yzz.common.storage;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

/**
 * 本地磁盘存储实现。
 *
 * <p>路径规则：{@code tenants/{租户标识}/{业务目录}/{uuid}{扩展名}}，跨租户路径一律拒绝，
 * 与对象存储驱动保持同一套键规则，切换驱动时历史路径不变。</p>
 */
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;
    private final Path root;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
        this.root = Paths.get(properties.getLocalRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("初始化本地存储目录失败：" + root, e);
        }
    }

    @Override
    public String driver() {
        return "local";
    }

    @Override
    public StoredFile upload(String tenantCode, String dir, String filename,
                             InputStream inputStream, long size, String contentType) {
        requireTenant(tenantCode);
        long maxBytes = (long) properties.getMaxSizeMb() * 1024 * 1024;
        if (size > maxBytes) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST,
                    "文件超过大小上限 " + properties.getMaxSizeMb() + " MB");
        }
        String objectKey = buildKey(tenantCode, dir, filename);
        Path target = resolveSafe(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "文件写入失败：" + e.getMessage());
        }
        return new StoredFile(objectKey, urlOf(objectKey), size);
    }

    @Override
    public Resource download(String tenantCode, String objectKey) {
        assertOwnership(tenantCode, objectKey);
        Path target = resolveSafe(objectKey);
        if (!Files.exists(target)) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "文件不存在或已被删除");
        }
        return new FileSystemResource(target);
    }

    @Override
    public String signedUrl(String tenantCode, String objectKey, Duration ttl) {
        assertOwnership(tenantCode, objectKey);
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        return urlOf(objectKey) + "?expireAt=" + expireAt;
    }

    @Override
    public void delete(String tenantCode, String objectKey) {
        assertOwnership(tenantCode, objectKey);
        try {
            Files.deleteIfExists(resolveSafe(objectKey));
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.STORAGE_ERROR, "文件删除失败：" + e.getMessage());
        }
    }

    private String buildKey(String tenantCode, String dir, String filename) {
        String cleanDir = dir == null ? "" : dir.replaceAll("^/+|/+$", "");
        String ext = "";
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && filename.length() - dot <= 10) {
                ext = filename.substring(dot).toLowerCase();
            }
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        return cleanDir.isEmpty()
                ? "tenants/" + tenantCode + "/" + name
                : "tenants/" + tenantCode + "/" + cleanDir + "/" + name;
    }

    private String urlOf(String objectKey) {
        String base = properties.getPublicBaseUrl().replaceAll("/+$", "");
        return base + "/" + objectKey;
    }

    private Path resolveSafe(String objectKey) {
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED);
        }
        return target;
    }

    private void requireTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED, "缺少租户标识，禁止写入存储");
        }
    }

    private void assertOwnership(String tenantCode, String objectKey) {
        requireTenant(tenantCode);
        if (objectKey == null || !objectKey.startsWith("tenants/" + tenantCode + "/")) {
            throw BusinessException.of(ErrorCode.STORAGE_PATH_DENIED);
        }
    }
}
