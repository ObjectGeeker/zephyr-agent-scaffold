package com.object.ai.auth.wechat.model.enums;

/**
 * 微信扫码登录票据状态
 */
public enum WeChatLoginTicketStatusEnum {

    /**
     * 已创建，等待扫码
     */
    PENDING,

    /**
     * 已扫码，等待用户回复验证码
     */
    WAITING_CODE,

    /**
     * 验证码确认成功
     */
    CONFIRMED,

    /**
     * 已过期（轮询时缓存不存在）
     */
    EXPIRED

}
