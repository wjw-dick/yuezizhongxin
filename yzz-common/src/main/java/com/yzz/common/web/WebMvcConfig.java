package com.yzz.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层装配：认证、租户生命周期、跨域。
 *
 * <p>认证拦截器覆盖全部 /api 接口，仅放行两个登录入口；租户生命周期拦截器覆盖租户端接口，
 * 登录入口需要放行，否则欠费租户将无法登录查看账单。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final TenantLifecycleInterceptor tenantLifecycleInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        TenantLifecycleInterceptor tenantLifecycleInterceptor) {
        this.authInterceptor = authInterceptor;
        this.tenantLifecycleInterceptor = tenantLifecycleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/tenant/auth/login",
                        "/api/platform/auth/login",
                        // 官网/小程序/H5 读取字典、语言包、登录方式，免登录
                        "/api/open/**",
                        "/error");
        registry.addInterceptor(tenantLifecycleInterceptor)
                .addPathPatterns("/api/tenant/**")
                .excludePathPatterns("/api/tenant/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Tenant-Code")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
