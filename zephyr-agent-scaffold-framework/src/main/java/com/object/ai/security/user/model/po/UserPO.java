package com.object.ai.security.user.model.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import com.object.ai.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户表 -- 只存储最基本的用户信息
 */
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user")
@Data
public class UserPO extends BasePO {

    @TableField("username")
    private String username;

    @TableField("account")
    private String account;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("password")
    private String password;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("wx_open_id")
    private String wxOpenId;

    @TableField(value = "user_roles", typeHandler = Jackson3TypeHandler.class)
    private List<String> userRoles;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

}
