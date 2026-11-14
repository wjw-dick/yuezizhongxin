package com.yzz.common.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.time.Duration;

/**
 * 统一存储门面。业务代码只依赖本接口，不直接调用厂商 SDK，便于 5 驱动切换。
 *
 * <p>实现必须强制校验对象键以 {@code tenants/{租户标识}/} 开头，跨租户路径一律拒绝。</p>
 */
public interface StorageService {

    StoredFile upload(String tenantCode, String dir, String filename,
                      InputStream inputStream, long size, String contentType);

    Resource download(String tenantCode, String objectKey);

    /** 生成带签名的临时访问地址，用于图片预览与附件下载。 */
    String signedUrl(String tenantCode, String objectKey, Duration ttl);

    void delete(String tenantCode, String objectKey);

    /** 驱动标识：local / s3 / minio / oss / cos。 */
    default String driver() {
        return "unknown";
    }

    /** 连接测试：写入并删除一个探测对象，失败时抛出带原因的异常。 */
    default void testConnection(String tenantCode) {
        StoredFile probe = upload(tenantCode, ".probe", "probe.txt",
                new java.io.ByteArrayInputStream("ok".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                2, "text/plain");
        delete(tenantCode, probe.objectKey());
    }
}
