package com.yzz.common.api;

/**
 * 全局错误码。
 *
 * <p>约定：4xxxx 为客户端错误，5xxxx 为服务端错误；跨租户越权固定使用 40300，
 * 便于验收脚本按错误码断言。</p>
 */
public enum ErrorCode {

    SUCCESS(0, "成功"),

    BAD_REQUEST(40000, "请求参数不合法"),
    SENSITIVE_WORD(40010, "内容命中敏感词，请修改后重试"),
    QUOTA_EXCEEDED(40020, "已达套餐额度上限，请升级套餐或调整配额"),

    UNAUTHORIZED(40100, "未登录或登录已过期"),
    BAD_CREDENTIALS(40101, "账号或密码错误"),
    TENANT_NOT_FOUND(40102, "租户不存在或已失效"),

    TENANT_ACCESS_DENIED(40300, "无权访问其他租户的数据"),
    TENANT_DISABLED(40301, "租户已停用，请联系平台运营方"),
    TENANT_EXPIRED(40302, "租户服务已到期，请联系平台运营方续期"),
    TENANT_ARREARS(40303, "租户存在欠费，写操作已被限制，请先完成缴费"),
    FORBIDDEN(40304, "当前账号无权执行该操作"),
    END_MISMATCH(40305, "平台账号与租户后台登录入口不互通"),

    NOT_FOUND(40400, "数据不存在"),
    CONFLICT(40900, "数据冲突，请刷新后重试"),
    SCHEDULE_CONFLICT(40901, "该房间该日期档期已被占用"),
    DUPLICATE_SUBMIT(40902, "请勿重复提交"),

    RATE_LIMITED(42900, "操作过于频繁，请稍后再试"),

    INTERNAL_ERROR(50000, "系统繁忙，请稍后再试"),
    STORAGE_ERROR(50010, "文件存储异常，请稍后重试"),
    STORAGE_PATH_DENIED(50011, "文件路径超出当前租户范围");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
