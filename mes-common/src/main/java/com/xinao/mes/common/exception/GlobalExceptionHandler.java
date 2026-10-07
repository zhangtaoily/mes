package com.xinao.mes.common.exception;

import com.xinao.mes.common.result.R;
import com.xinao.mes.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：正常业务校验失败，warn 级别即可 */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("业务异常 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@RequestBody @Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数错误";
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /** 参数校验异常（表单绑定） */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数错误";
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "请求方式不支持: " + e.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNotFound(NoResourceFoundException e) {
        return R.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底：未知异常一律 500，堆栈进日志，不外泄给前端 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e, HttpServletRequest request) {
        log.error("系统异常 {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ResultCode.ERROR);
    }
}
