package com.xinao.mes.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体
 */
@Data
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> fail(String msg) {
        return build(ResultCode.ERROR.getCode(), msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMsg(), null);
    }

    public static <T> R<T> build(int code, String msg, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
