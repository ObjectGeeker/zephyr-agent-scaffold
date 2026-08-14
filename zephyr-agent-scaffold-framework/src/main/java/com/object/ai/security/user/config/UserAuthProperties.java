package com.object.ai.security.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户认证配置
 */
@Data
@ConfigurationProperties(prefix = "security.user")
public class UserAuthProperties {

    /**
     * 是否开启用户注册功能
     */
    private boolean registerEnabled = false;
}
