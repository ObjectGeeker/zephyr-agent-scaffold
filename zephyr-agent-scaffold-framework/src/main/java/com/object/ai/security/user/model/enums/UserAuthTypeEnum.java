package com.object.ai.security.user.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户认证类型
 */
@Getter
@AllArgsConstructor
public enum UserAuthTypeEnum {

    /**
     * 账号认证
     */
    ACCOUNT("ACCOUNT", "账号");

    private final String code;

    private final String desc;

    /**
     * 判断认证类型是否匹配
     *
     * @param type 待匹配的认证类型
     * @return 是否匹配
     */
    public boolean matches(String type) {
        return code.equalsIgnoreCase(type);
    }
}
