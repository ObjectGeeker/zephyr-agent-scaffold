package com.object.ai.security.user.service.impl;

import com.object.ai.common.base.BizException;
import com.object.ai.common.base.ErrorCode;
import com.object.ai.security.user.config.UserAuthProperties;
import com.object.ai.security.user.model.request.UserLoginRequest;
import com.object.ai.security.user.model.request.UserRegisterRequest;
import com.object.ai.security.user.model.response.UserAuthResult;
import com.object.ai.security.user.service.UserAuthService;
import com.object.ai.security.user.service.strategy.UserAuthStrategyRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户认证服务实现
 */
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserAuthStrategyRouter strategyRouter;

    private final UserAuthProperties userAuthProperties;

    @Override
    public UserAuthResult login(UserLoginRequest request) {
        String loginType = request == null ? null : request.getLoginType();
        return strategyRouter.routeLogin(loginType).login(request);
    }

    @Override
    @Transactional
    public UserAuthResult register(UserRegisterRequest request) {
        if (!userAuthProperties.isRegisterEnabled()) {
            throw BizException.of(ErrorCode.OPERATION_NOT_ALLOWED, "用户注册功能未开启");
        }
        String registerType = request == null ? null : request.getRegisterType();
        return strategyRouter.routeRegister(registerType).register(request);
    }
}
