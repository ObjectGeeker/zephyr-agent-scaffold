package com.object.ai.security.user.service;

import com.object.ai.security.user.model.request.UserLoginRequest;
import com.object.ai.security.user.model.request.UserRegisterRequest;
import com.object.ai.security.user.model.response.UserAuthResult;

/**
 * 用户认证服务
 */
public interface UserAuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证结果
     */
    UserAuthResult login(UserLoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证结果
     */
    UserAuthResult register(UserRegisterRequest request);
}
