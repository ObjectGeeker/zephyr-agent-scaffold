package com.object.ai.auth.user.model.po;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.object.ai.auth.user.model.enums.UserRoleEnum;
import com.object.ai.auth.user.model.enums.UserStatusEnum;
import com.object.ai.auth.user.model.vo.UserVO;
import com.object.ai.common.po.BasePO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "tb_user", autoResultMap = true)
public class UserPO extends BasePO {
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
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> roles;
    /**
     * 用户状态
     */
    private String status;
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

    public UserVO toUserVO() {
        UserVO userVO = BeanUtil.copyProperties(this, UserVO.class, "status", "roles");
        userVO.setStatus(UserStatusEnum.valueOf(this.status));
        userVO.setRoles(this.roles.stream().map(UserRoleEnum::valueOf).toList());
        return userVO;
    }

}
