package com.object.ai.auth.wechat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信扫码登录二维码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信扫码登录二维码")
public class WeChatQrCodeVO {

    @Schema(description = "登录票据 ID")
    private String ticketId;

    @Schema(description = "二维码图片 URL")
    private String qrCodeUrl;

    @Schema(description = "有效期（秒）")
    private Integer expireSeconds;

}
