package com.yzz.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档。访问 /swagger-ui.html 查看；所有接口统一 Bearer 令牌鉴权。
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "BearerAuth";

    @Bean
    public OpenAPI yzzOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("月子中心数字化多端运营平台 接口文档")
                        .version("1.0.0")
                        .description("多租户 SaaS：平台运营端与租户后台接口物理隔离，"
                                + "租户接口需在请求头携带 X-Tenant-Code 或使用租户子域名访问")
                        .contact(new Contact().name("研发组")))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("登录接口返回的 token 原样填入")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
