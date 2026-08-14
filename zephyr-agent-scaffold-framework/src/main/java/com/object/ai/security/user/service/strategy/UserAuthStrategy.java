package com.object.ai.security.user.service.strategy;

import com.object.ai.security.user.model.request.UserLoginRequest;
import com.object.ai.security.user.model.request.UserRegisterRequest;
import com.object.ai.security.user.model.response.UserAuthResult;
import com.object.ai.security.user.model.po.UserPO;

/**
 * 用户认证策略
 */
public interface UserAuthStrategy {

    /**
     * 判断是否支持登录类型
     *
     * @param loginType 登录类型
     * @return 是否支持
     */
    boolean supportsLogin(String loginType);

    /**
     * 判断是否支持注册类型
     *
     * @param registerType 注册类型
     * @return 是否支持
     */
    boolean supportsRegister(String registerType);

    /**
     * 执行登录
     *
     * @param request 登录请求
     * @return 认证结果
     */
    UserAuthResult login(UserLoginRequest request);

    /**
     * 执行注册
     *
     * @param request 注册请求
     * @return 认证结果
     */
    UserAuthResult register(UserRegisterRequest request);

    /**
     * 校验登录凭证并查询用户
     *
     * @param request 登录请求
     * @return 用户持久化对象
     */
    UserPO authenticate(UserLoginRequest request);

    /**
     * 创建用户
     *
     * @param request 注册请求
     * @return 用户持久化对象
     */
    UserPO registerUser(UserRegisterRequest request);
}
