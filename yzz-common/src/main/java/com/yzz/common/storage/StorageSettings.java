package com.yzz.common.storage;

/**
 * 单个存储驱动的连接参数。
 *
 * <p>S3 / MinIO / 阿里云 OSS / 腾讯云 COS 统一走 S3 兼容协议，
 * 差异只在 endpoint、region 与是否 path-style，因此共用一套实现。</p>
 */
public class StorageSettings {

    /** local / s3 / minio / oss / cos。 */
    private String driver = "local";

    /** 服务地址，如 https://oss-cn-hangzhou.aliyuncs.com。 */
    private String endpoint;

    private String region = "us-east-1";

    private String bucket;

    private String accessKey;

    private String secretKey;

    /** MinIO 与自建网关需开启 path-style；云厂商默认 virtual-host 也可以走 path-style。 */
    private boolean pathStyle = true;

    /** 对外访问前缀，留空时用 endpoint/bucket 拼接。 */
    private String publicBaseUrl;

    /** 本地驱动根目录。 */
    private String localRoot = "./data/storage";

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(boolean pathStyle) {
        this.pathStyle = pathStyle;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }
}
