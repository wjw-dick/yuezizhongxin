package com.yzz.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志埋点。
 *
 * <p>登录、配置变更、订单与退款、护理记录修改、导出、菜单发布、凭证修改等写操作必须标注。
 * 审计日志只读，不提供新增、编辑、删除接口。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 模块中文名，如「租户管理」。 */
    String module();

    /** 动作中文名，如「开通租户」。 */
    String action();

    /** 是否记录请求参数（涉及密钥、密码的接口请置为 false）。 */
    boolean withArgs() default true;
}
