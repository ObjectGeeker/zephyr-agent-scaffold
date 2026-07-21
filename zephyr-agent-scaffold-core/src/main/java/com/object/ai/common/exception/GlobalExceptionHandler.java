package com.object.ai.common.exception;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotSafeException;
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

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<String> notLoginExceptionHandler(NotLoginException e) {
        log.info("未登录: type={}, message={}", e.getType(), e.getMessage());
        return resolveNotLogin(e);
    }

    @ExceptionHandler(NotRoleException.class)
    public BaseResponse<String> notRoleExceptionHandler(NotRoleException e) {
        log.info("无角色: role={}", e.getRole());
        return ResultUtil.error(BizErrorCode.FORBIDDEN_ERROR.getCode(), "无此角色：" + e.getRole());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<String> notPermissionExceptionHandler(NotPermissionException e) {
        log.info("无权限: permission={}", e.getPermission());
        return ResultUtil.error(BizErrorCode.NO_PERMISSION_ERROR.getCode(), "无此权限：" + e.getPermission());
    }

    @ExceptionHandler(DisableServiceException.class)
    public BaseResponse<String> disableServiceExceptionHandler(DisableServiceException e) {
        log.info("账号封禁: service={}, disableTime={}", e.getService(), e.getDisableTime());
        String message = e.getDisableTime() > 0
                ? "账号已被封禁，" + e.getDisableTime() + " 秒后解封"
                : "账号已被封禁";
        return ResultUtil.error(BizErrorCode.FORBIDDEN_ERROR.getCode(), message);
    }

    @ExceptionHandler(NotSafeException.class)
    public BaseResponse<String> notSafeExceptionHandler(NotSafeException e) {
        log.info("二级认证未通过: {}", e.getMessage());
        return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "需要二级认证");
    }

    private BaseResponse<String> resolveNotLogin(NotLoginException e) {
        String type = e.getType();
        if (NotLoginException.NOT_TOKEN.equals(type)) {
            return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "未提供 Token");
        }
        if (NotLoginException.INVALID_TOKEN.equals(type)) {
            return ResultUtil.error(BizErrorCode.TOKEN_INVALID_ERROR.getCode(), "Token 无效");
        }
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            return ResultUtil.error(BizErrorCode.TOKEN_EXPIRED_ERROR.getCode(), "Token 已过期，请重新登录");
        }
        if (NotLoginException.BE_REPLACED.equals(type)) {
            return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "账号已在其他设备登录");
        }
        if (NotLoginException.KICK_OUT.equals(type)) {
            return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "已被强制下线");
        }
        if (NotLoginException.TOKEN_FREEZE.equals(type)) {
            return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "Token 已被冻结");
        }
        if (NotLoginException.NO_PREFIX.equals(type)) {
            return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "未按指定前缀提交 Token");
        }
        return ResultUtil.error(BizErrorCode.UNAUTHORIZED_ERROR.getCode(), "未登录或登录已失效");
    }

}
