package com.yzz.common.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储装配。
 *
 * <p>本地驱动始终注册为兜底 Bean（名为 localStorageService）；租户在后台切换驱动后，
 * 由业务模块的租户存储路由以 @Primary 方式覆盖，业务代码注入 {@link StorageService} 即可。</p>
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public StorageService localStorageService(StorageProperties properties) {
        return new LocalStorageService(properties);
    }
}
