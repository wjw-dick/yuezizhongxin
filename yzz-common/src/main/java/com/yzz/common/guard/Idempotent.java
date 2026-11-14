package com.yzz.common.guard;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口防重复提交（幂等）。预约、下单、支付回调、护理记录提交必须标注。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等键前缀，为空时按「请求路径 + 登录主体 + 入参」自动生成。 */
    String key() default "";

    /** 幂等窗口，单位秒。 */
    int ttlSeconds() default 10;
}
