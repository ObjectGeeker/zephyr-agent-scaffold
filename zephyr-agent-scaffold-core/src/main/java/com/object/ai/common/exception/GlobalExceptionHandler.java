package com.object.ai.common.exception;

import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Hidden
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<String> businessExceptionHandler(BusinessException e) {
        log.info("{}", e.getMessage(), e);
        return ResultUtil.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<String> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        // 从 BindingResult 中提取所有字段级别的校验错误
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                // getDefaultMessage() 会精准返回 @NotBlank 注解中配置的 message 内容
                .collect(Collectors.joining("; "));

        return ResultUtil.error(BizErrorCode.PARAMS_ERROR.getCode(), errorMsg);
    }

}
