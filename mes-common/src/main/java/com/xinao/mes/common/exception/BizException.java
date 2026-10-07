package com.xinao.mes.common.exception;

import com.xinao.mes.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：业务规则校验失败时抛出，由全局异常处理器转为统一返回体
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMsg());
    }
}
