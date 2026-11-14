package com.yzz.common.tenant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略租户条件。
 *
 * <p>仅允许用于平台级查询或跨租户汇总查询，标注后 MyBatis 租户插件不再追加 tenant_id 条件。
 * 使用位置必须在代码评审中登记，避免成为越权入口。</p>
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {

    /** 忽略原因，必填，便于审计与评审。 */
    String value();
}
