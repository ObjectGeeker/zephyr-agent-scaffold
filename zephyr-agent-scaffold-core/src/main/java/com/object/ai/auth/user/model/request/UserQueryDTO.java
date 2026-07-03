package com.object.ai.auth.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户查询请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserQueryDTO {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态：ACTIVE、BAN
     */
    private String status;

    /**
     * 用户角色：ADMIN、USER
     */
    private String role;

}
