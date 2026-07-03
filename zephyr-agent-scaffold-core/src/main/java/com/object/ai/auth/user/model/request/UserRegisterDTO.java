package com.object.ai.auth.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO {

    /**
     * 注册类型：account, email, phone
     */
    private String registerType;

    /**
     * 注册账号
     */
    private String registerAccount;

    /**
     * 注册凭证：verifyCode or password
     */
    private String registerCertificate;

    /**
     * 用户名
     */
    private String username;

    /**
     * 微信 openid（用于微信扫码注册）
     */
    private String wxOpenid;

}
