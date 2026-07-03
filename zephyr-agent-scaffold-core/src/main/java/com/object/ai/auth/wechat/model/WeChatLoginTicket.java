package com.object.ai.auth.wechat.model;

import com.object.ai.auth.wechat.model.enums.WeChatLoginTicketStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 微信扫码登录内存凭据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatLoginTicket {

    private String ticketId;

    private String sceneStr;

    private String openid;

    private String verifyCode;

    private WeChatLoginTicketStatusEnum status;

    private LocalDateTime createTime;

    private LocalDateTime confirmTime;

}
