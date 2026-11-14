package com.yzz.common.storage;

/**
 * 上传结果。
 *
 * @param objectKey 对象键，形如 tenants/{租户标识}/room/2026/uuid.jpg
 * @param url       可访问地址
 * @param size      字节数
 */
public record StoredFile(String objectKey, String url, long size) {
}
