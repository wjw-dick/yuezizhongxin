package com.yzz.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一返回结构。
 *
 * <p>后端只暴露 REST 接口，全部响应统一为 code/msg/data 三段式；
 * code 为 0 表示成功，非 0 时 msg 为可直接展示给用户的中文提示。</p>
 */
@Schema(description = "统一返回结构")
public class R<T> {

    @Schema(description = "业务码，0 表示成功")
    private int code;

    @Schema(description = "提示信息，可直接对用户展示")
    private String msg;

    @Schema(description = "业务数据")
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String msg) {
        return new R<>(errorCode.getCode(), msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
