package com.yzz.common.guard;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流。默认单用户 1 分钟 5 次，用于预约参观、下单、支付、护理记录提交等写接口。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许次数。 */
    int count() default 5;

    /** 窗口长度，单位秒。 */
    int seconds() default 60;
}
