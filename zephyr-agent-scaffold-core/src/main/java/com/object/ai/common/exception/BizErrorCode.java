package com.object.ai.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码。
 *
 * <p>编码约定：
 * <ul>
 *   <li>{@code 200}：成功</li>
 *   <li>{@code 401xx}：认证/登录</li>
 *   <li>{@code 400xx}：参数/校验</li>
 *   <li>{@code 403xx}：权限</li>
 *   <li>{@code 404xx}：资源不存在</li>
 *   <li>{@code 405xx / 409xx / 429xx}：请求方式、冲突、限流</li>
 *   <li>{@code 500xx / 503xx / 504xx}：服务端、不可用、远程调用</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum BizErrorCode {

    SUCCESS(200, "success"),

    UNAUTHORIZED_ERROR(40100, "未登录或登录已失效"),
    TOKEN_INVALID_ERROR(40101, "Token 无效"),
    TOKEN_EXPIRED_ERROR(40102, "Token 已过期"),

    PARAMS_ERROR(40000, "参数错误"),
    VALIDATION_ERROR(40001, "参数校验失败"),
    PARAMS_MISSING_ERROR(40002, "缺少必要参数"),

    FORBIDDEN_ERROR(40300, "禁止操作"),
    NO_PERMISSION_ERROR(40301, "无访问权限"),

    NOT_FOUND_ERROR(40400, "数据不存在"),

    METHOD_NOT_ALLOWED_ERROR(40500, "请求方法不支持"),

    DATA_EXIST_ERROR(40900, "数据已存在"),
    DUPLICATE_SUBMIT_ERROR(40901, "请勿重复提交"),

    TOO_MANY_REQUESTS_ERROR(42900, "请求过于频繁，请稍后再试"),

    OPERATION_ERROR(50000, "操作失败"),
    SYSTEM_ERROR(50001, "系统异常，请稍后再试"),
    SYSTEM_INNER_ERROR(50002, "系统内部错误"),

    SERVICE_UNAVAILABLE_ERROR(50300, "服务暂不可用"),
    REMOTE_CALL_ERROR(50400, "远程服务调用失败");

    private final Integer code;
    private final String message;

}
