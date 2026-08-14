package com.object.ai.security.user.model.response;

import com.object.ai.security.user.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户认证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResult {

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * Token 名称
     */
    private String tokenName;

    /**
     * Token 值
     */
    private String tokenValue;
}
