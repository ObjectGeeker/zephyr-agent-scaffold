package com.object.ai.auth.user.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.object.ai.auth.user.model.enums.UserRoleEnum;
import com.object.ai.auth.user.model.enums.UserStatusEnum;
import com.object.ai.auth.user.model.po.UserPO;
import com.object.ai.common.vo.BaseVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserVO extends BaseVO {
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 微信 openid（用于微信扫码登录）
     */
    private String wxOpenid;
    /**
     * 密码
     */
    private String password;
    /**
     * 用户名
     */
    private String username;
    /**
     * 角色列表
     */
    private List<UserRoleEnum> roles;
    /**
     * 用户状态
     */
    private UserStatusEnum status;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 头像url
     */
    private String avatar;
    /**
     * 上一次登录时间
     */
    private LocalDateTime lastLoginTime;

    public UserPO toUserPO() {
        UserPO userPO = BeanUtil.copyProperties(this, UserPO.class, "status", "roles");
        userPO.setStatus(this.status.name());
        userPO.setRoles(this.roles.stream().map(UserRoleEnum::name).toList());
        return userPO;
    }
}
