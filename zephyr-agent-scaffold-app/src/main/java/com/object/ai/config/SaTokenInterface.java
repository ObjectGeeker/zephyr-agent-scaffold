package com.object.ai.config;

import cn.dev33.satoken.model.wrapperInfo.SaDisableWrapperInfo;
import cn.dev33.satoken.stp.StpInterface;
import com.object.ai.security.user.mapper.UserMapper;
import com.object.ai.security.user.model.po.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色和权限数据提供实现
 */
@Component
@RequiredArgsConstructor
public class SaTokenInterface implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return getUserRoles(loginId);
    }

    @Override
    public List<String> getRoleList(Object userId, String loginType) {
        return getUserRoles(userId);
    }

    @Override
    public SaDisableWrapperInfo isDisabled(Object loginId, String service) {
        if (loginId == null || userMapper.selectById(String.valueOf(loginId)) == null) {
            return SaDisableWrapperInfo.createDisabled(-1, 1);
        }
        return SaDisableWrapperInfo.createNotDisabled();
    }

    /**
     * 查询用户绑定的角色列表
     *
     * @param loginId 登录用户 ID
     * @return 用户角色列表，同时作为权限列表使用
     */
    private List<String> getUserRoles(Object loginId) {
        if (loginId == null) {
            return Collections.emptyList();
        }

        UserPO user = userMapper.selectById(String.valueOf(loginId));
        if (user == null || user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<>(user.getUserRoles()));
    }
}
