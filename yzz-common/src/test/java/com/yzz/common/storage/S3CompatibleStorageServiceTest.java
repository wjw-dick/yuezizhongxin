package com.yzz.common.storage;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S3 兼容驱动：签名地址结构、路径规则与跨租户拦截。
 */
class S3CompatibleStorageServiceTest {

    private S3CompatibleStorageService service() {
        StorageSettings settings = new StorageSettings();
        settings.setDriver("minio");
        settings.setEndpoint("http://127.0.0.1:9000");
        settings.setRegion("cn-hangzhou");
        settings.setBucket("yzz-files");
        settings.setAccessKey("AKIDEXAMPLE");
        settings.setSecretKey("SECRETEXAMPLE");
        settings.setPathStyle(true);
        return new S3CompatibleStorageService(settings, 20);
    }

    @Test
    void signedUrlContainsSigV4ParametersAndUsesTenantPath() {
        String url = service().signedUrl("demo", "tenants/demo/room/a.jpg", Duration.ofMinutes(5));
        assertTrue(url.startsWith("http://127.0.0.1:9000/yzz-files/tenants/demo/room/a.jpg?"), url);
        assertTrue(url.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"), url);
        assertTrue(url.contains("X-Amz-Credential=AKIDEXAMPLE%2F"), url);
        assertTrue(url.contains("X-Amz-Expires=300"), url);
        assertTrue(url.contains("X-Amz-SignedHeaders=host"), url);
        String signature = url.substring(url.indexOf("X-Amz-Signature=") + 16);
        assertEquals(64, signature.length(), "签名应为 64 位十六进制");
        assertTrue(signature.matches("[0-9a-f]{64}"), signature);
    }

    @Test
    void signedUrlIsStableForSameMinuteAndKey() {
        String first = service().signedUrl("demo", "tenants/demo/a.jpg", Duration.ofMinutes(5));
        String second = service().signedUrl("demo", "tenants/demo/a.jpg", Duration.ofMinutes(5));
        assertEquals(first.substring(first.indexOf("X-Amz-Signature=")),
                second.substring(second.indexOf("X-Amz-Signature=")));
    }

    @Test
    void crossTenantKeyIsRejectedBeforeAnyNetworkCall() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().download("demo", "tenants/other/a.jpg"));
        assertEquals(ErrorCode.STORAGE_PATH_DENIED, ex.getErrorCode());
        assertEquals("minio", service().driver());
    }

    @Test
    void emptyTenantIsRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().delete("", "tenants/demo/a.jpg"));
        assertEquals(ErrorCode.STORAGE_PATH_DENIED, ex.getErrorCode());
    }

    @Test
    void missingEndpointFailsFast() {
        StorageSettings settings = new StorageSettings();
        settings.setDriver("s3");
        settings.setBucket("b");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new S3CompatibleStorageService(settings, 20));
        assertEquals(ErrorCode.STORAGE_ERROR, ex.getErrorCode());
    }
}
