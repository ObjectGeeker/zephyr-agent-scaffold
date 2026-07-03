package com.object.ai.auth.wechat.config;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 微信公众号 SDK 配置
 */
@Configuration
@EnableConfigurationProperties(WeChatMpProperties.class)
public class WeChatMpConfiguration {

    @Bean
    public WxMpService wxMpService(WeChatMpProperties properties) {
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(properties.getAppId());
        config.setSecret(properties.getSecret());
        config.setToken(properties.getToken());
        if (StringUtils.hasText(properties.getAesKey())) {
            config.setAesKey(properties.getAesKey());
        }
        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(config);
        return wxMpService;
    }

}
