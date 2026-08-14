package com.object.ai.security.user.model.request;

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
public class UserRegisterRequest {

    /**
     * 注册类型
     */
    private String registerType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 登录密码
     */
    private String password;
}
