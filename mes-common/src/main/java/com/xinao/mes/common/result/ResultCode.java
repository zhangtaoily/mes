package com.xinao.mes.common.result;

import lombok.Getter;

/**
 * 统一返回码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无操作权限"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "系统异常");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
