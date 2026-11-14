package com.yzz.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置。驱动可切换，密钥在配置文件中以环境变量注入，不入代码库。
 */
@ConfigurationProperties(prefix = "yzz.storage")
public class StorageProperties {

    /** 驱动：local / s3 / oss / cos / minio。 */
    private String driver = "local";

    /** 本地磁盘模式下的根目录。 */
    private String localRoot = "./data/storage";

    /** 对外访问前缀，用于拼接可预览地址。 */
    private String publicBaseUrl = "/files";

    /** 单文件大小上限（MB），超出直接拒绝。 */
    private int maxSizeMb = 20;

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public int getMaxSizeMb() {
        return maxSizeMb;
    }

    public void setMaxSizeMb(int maxSizeMb) {
        this.maxSizeMb = maxSizeMb;
    }
}
