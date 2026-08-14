package com.object.ai.security.user.model.vo;

import com.object.ai.common.base.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserVO extends BaseVO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 微信 OpenID
     */
    private String wxOpenId;

    /**
     * 用户角色列表
     */
    private List<String> userRoles;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

}
