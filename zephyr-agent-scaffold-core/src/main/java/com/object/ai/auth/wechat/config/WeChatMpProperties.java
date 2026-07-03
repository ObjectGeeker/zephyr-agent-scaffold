package com.object.ai.auth.wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信公众号配置
 */
@Data
@ConfigurationProperties(prefix = "wechat.mp")
public class WeChatMpProperties {

    /**
     * 测试号 AppId
     */
    private String appId;

    /**
     * 测试号 AppSecret
     */
    private String secret;

    /**
     * 服务器配置 Token，需与公众平台一致
     */
    private String token;

    /**
     * 消息加解密密钥，明文模式可留空
     */
    private String aesKey;

    /**
     * 临时二维码有效期（秒）
     */
    private int qrcodeExpireSeconds = 300;

}
