package com.object.ai.common.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public BaseResponse<String> bizExceptionHandler(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage(), e);
        return fail(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<String> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("参数校验异常: {}", e.getMessage(), e);
        return fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<String> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage(), e);
        return fail(ErrorCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<String> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式不支持: {}", e.getMessage());
        return fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public BaseResponse<String> noResourceFoundExceptionHandler(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return fail(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<String> exceptionHandler(Exception e) {
        log.error("系统异常", e);
        return fail(ErrorCode.SYSTEM_ERROR);
    }

    private <T> BaseResponse<T> fail(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), false, errorCode.getMessage());
    }

    private <T> BaseResponse<T> fail(Integer code, String message) {
        return new BaseResponse<>(code, false, message);
    }

}
