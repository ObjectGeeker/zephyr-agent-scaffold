package com.object.ai.auth.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {

    /**
     * 登录类型：account, email, phone
     */
    private String loginType;

    /**
     * 登录账号
     */
    private String loginAccount;

    /**
     * 登录凭证：password
     */
    private String loginCertificate;

}
