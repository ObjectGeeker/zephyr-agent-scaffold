package com.object.ai.security.user.service.strategy;

import com.object.ai.common.base.BizException;
import com.object.ai.common.base.ErrorCode;
import com.object.ai.security.user.model.enums.UserAuthTypeEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 用户认证策略路由
 */
@Component
public class UserAuthStrategyRouter {

    private final Map<UserAuthTypeEnum, UserAuthStrategy> loginStrategies;

    private final Map<UserAuthTypeEnum, UserAuthStrategy> registerStrategies;

    public UserAuthStrategyRouter(List<UserAuthStrategy> strategies) {
        this.loginStrategies = buildStrategyMap(strategies, true);
        this.registerStrategies = buildStrategyMap(strategies, false);
    }

    /**
     * 路由登录策略
     *
     * @param loginType 登录类型
     * @return 登录策略
     */
    public UserAuthStrategy routeLogin(String loginType) {
        return route(loginType, loginStrategies, "登录");
    }

    /**
     * 路由注册策略
     *
     * @param registerType 注册类型
     * @return 注册策略
     */
    public UserAuthStrategy routeRegister(String registerType) {
        return route(registerType, registerStrategies, "注册");
    }

    private Map<UserAuthTypeEnum, UserAuthStrategy> buildStrategyMap(
            List<UserAuthStrategy> strategies, boolean login) {
        Map<UserAuthTypeEnum, UserAuthStrategy> result = new EnumMap<>(UserAuthTypeEnum.class);
        for (UserAuthTypeEnum type : UserAuthTypeEnum.values()) {
            List<UserAuthStrategy> matchedStrategies = strategies.stream()
                    .filter(strategy -> login
                            ? strategy.supportsLogin(type.getCode())
                            : strategy.supportsRegister(type.getCode()))
                    .toList();
            if (matchedStrategies.size() > 1) {
                throw new IllegalStateException("认证类型存在多个策略：" + type.getCode());
            }
            if (!matchedStrategies.isEmpty()) {
                result.put(type, matchedStrategies.get(0));
            }
        }
        return result;
    }

    private UserAuthStrategy route(
            String type, Map<UserAuthTypeEnum, UserAuthStrategy> strategies, String operation) {
        if (!StringUtils.hasText(type)) {
            throw BizException.of(ErrorCode.PARAM_MISSING, operation + "类型不能为空");
        }

        UserAuthTypeEnum authType;
        try {
            authType = UserAuthTypeEnum.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "不支持的" + operation + "类型：" + type);
        }

        UserAuthStrategy strategy = strategies.get(authType);
        if (strategy == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "未配置" + operation + "策略：" + type);
        }
        return strategy;
    }
}
