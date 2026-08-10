package com.object.ai.common.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(200, "响应成功", ""),

    // ------------------------------ 系统通用类 ------------------------------
    SYSTEM_ERROR(500, "系统内部错误", "系统内部错误，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务不可用", "服务暂时不可用，请稍后重试"),
    REQUEST_TIMEOUT(504, "请求超时", "请求超时，请稍后重试"),
    TOO_MANY_REQUESTS(429, "请求过于频繁", "请求过于频繁，请稍后重试"),

    // --------------------------- 客户端 / 参数校验类 ---------------------------
    PARAM_ERROR(400, "参数错误", "请求参数不合法"),
    PARAM_MISSING(400, "参数缺失", "必填参数缺失"),
    UNAUTHORIZED(401, "未认证", "未登录或登录已过期"),
    FORBIDDEN(403, "无权限", "无权限访问该资源"),
    NOT_FOUND(404, "资源不存在", "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持", "不支持的请求方式"),
    REPEAT_SUBMIT(409, "重复提交", "请勿重复提交"),
    DATA_CONFLICT(409, "数据冲突", "数据存在冲突，请检查后重试"),

    // ------------------------------- 业务类 -------------------------------
    BUSINESS_ERROR(1000, "业务处理失败", "业务处理失败"),
    DATA_NOT_FOUND(1001, "数据不存在", "未查询到对应数据"),
    OPERATION_NOT_ALLOWED(1002, "操作不允许", "当前状态不允许该操作"),
    THIRD_PARTY_ERROR(1003, "第三方服务异常", "第三方服务调用失败"),

    // ------------------------- AI Agent / 模型调用类 -------------------------
    AGENT_NOT_FOUND(2001, "智能体不存在", "指定的智能体不存在"),
    AGENT_EXECUTE_ERROR(2002, "智能体执行失败", "智能体执行异常"),
    SESSION_NOT_FOUND(2003, "会话不存在", "会话不存在，请先创建会话"),
    MODEL_INVOKE_ERROR(2004, "模型调用失败", "大模型调用失败"),
    MODEL_CONFIG_ERROR(2005, "模型配置错误", "模型配置不合法（如 baseUrl / API Key 缺失）"),
    MCP_INVOKE_ERROR(2006, "MCP 工具调用失败", "MCP 工具调用异常"),
    STREAM_INTERRUPTED(2007, "流式响应中断", "流式响应被中断"),
    ;

    private final Integer code;

    private final String desc;

    private final String message;

}
