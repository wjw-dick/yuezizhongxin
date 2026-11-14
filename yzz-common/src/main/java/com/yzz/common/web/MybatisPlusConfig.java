package com.yzz.common.web;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.yzz.common.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 拦截器装配：租户条件强制注入 + 分页。
 *
 * <p>租户条件由插件统一追加，业务 Mapper 不需要手写 tenant_id；
 * 平台级查询通过平台上下文（isPlatform）或表前缀 platform_ 自动跳过。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /** 不参与租户隔离的全局表（平台级与全局字典、语言包）。 */
    private static final Set<String> GLOBAL_TABLES = Set.of(
            "sys_global_config",
            "sys_language",
            "sys_i18n_message",
            "sys_dict_category",
            "sys_dict_item");

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                // 上下文缺失时使用 -1，保证查询结果为空而不是返回全部租户数据
                return new LongValue(tenantId == null ? -1L : tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                if (TenantContext.isPlatform()) {
                    return true;
                }
                String name = tableName.toLowerCase();
                return name.startsWith("platform_") || GLOBAL_TABLES.contains(name);
            }
        }));
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
