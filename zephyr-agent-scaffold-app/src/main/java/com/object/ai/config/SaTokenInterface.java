package com.object.ai.config;

import cn.dev33.satoken.model.wrapperInfo.SaDisableWrapperInfo;
import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.collection.CollUtil;
import com.object.ai.auth.user.mapper.UserMapper;
import com.object.ai.auth.user.model.enums.UserStatusEnum;
import com.object.ai.auth.user.model.po.UserPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaTokenInterface implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return null;
    }

    @Override
    public List<String> getRoleList(Object userId, String loginType) {
        UserPO userPO = userMapper.selectById(String.valueOf(userId));
        if (userPO == null || CollUtil.isEmpty(userPO.getRoles())) {
            return CollUtil.newArrayList();
        }
        return userPO.getRoles();
    }

    @Override
    public SaDisableWrapperInfo isDisabled(Object loginId, String service) {
        UserPO userPO = userMapper.selectById(String.valueOf(loginId));
        if (userPO == null || CollUtil.isEmpty(userPO.getRoles())) {
            return null;
        }
        String status = userPO.getStatus();
        if (UserStatusEnum.BAN.name().equals(status)) {
            return new SaDisableWrapperInfo(true, -1, 1);
        }
        return new SaDisableWrapperInfo(false, 0, 0);
    }
}
