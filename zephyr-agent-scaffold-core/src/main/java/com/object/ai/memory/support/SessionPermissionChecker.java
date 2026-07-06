package com.object.ai.memory.support;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.object.ai.auth.user.model.enums.UserRoleEnum;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.memory.mapper.SessionMapper;
import com.object.ai.memory.model.po.SessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionPermissionChecker {

    private final SessionMapper sessionMapper;

    public SessionPO getSessionOrThrow(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "会话 ID 不能为空");
        }
        SessionPO sessionPO = sessionMapper.selectById(sessionId);
        if (sessionPO == null) {
            throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        return sessionPO;
    }

    public SessionPO checkOwner(String sessionId) {
        SessionPO sessionPO = getSessionOrThrow(sessionId);
        StpUtil.checkLogin();
        String currentUserId = StpUtil.getLoginIdAsString();
        boolean isOwner = StrUtil.equals(currentUserId, sessionPO.getUserId());
        boolean isAdmin = StpUtil.hasRole(UserRoleEnum.ADMIN.name());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(BizErrorCode.NO_PERMISSION_ERROR, "无权限访问该会话");
        }
        return sessionPO;
    }
}
