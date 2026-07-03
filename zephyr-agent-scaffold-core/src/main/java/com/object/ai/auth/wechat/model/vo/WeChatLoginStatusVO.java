package com.object.ai.auth.wechat.model.vo;

import com.object.ai.auth.wechat.model.enums.WeChatLoginTicketStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信扫码登录状态响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信扫码登录状态")
public class WeChatLoginStatusVO {

    @Schema(description = "登录票据 ID")
    private String ticketId;

    @Schema(description = "登录状态")
    private WeChatLoginTicketStatusEnum status;

    @Schema(description = "微信 openid，仅 CONFIRMED 时有值")
    private String wxOpenid;

}
