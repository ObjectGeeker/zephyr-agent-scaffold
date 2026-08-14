package com.object.ai.security.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 用户认证配置注册
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserAuthProperties.class)
public class UserAuthConfiguration {
}
