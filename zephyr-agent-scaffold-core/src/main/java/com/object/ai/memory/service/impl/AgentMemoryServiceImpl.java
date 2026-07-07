package com.object.ai.memory.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.memory.mapper.MessageMapper;
import com.object.ai.memory.mapper.SessionMapper;
import com.object.ai.memory.model.enums.SessionStatusEnum;
import com.object.ai.memory.model.po.MessagePO;
import com.object.ai.memory.model.po.SessionPO;
import com.object.ai.memory.model.request.MessageQueryDTO;
import com.object.ai.memory.model.request.SessionCreateDTO;
import com.object.ai.memory.model.request.SessionDeleteDTO;
import com.object.ai.memory.model.request.SessionQueryDTO;
import com.object.ai.memory.model.request.SessionUpdateDTO;
import com.object.ai.memory.model.vo.MessageVO;
import com.object.ai.memory.model.vo.SessionVO;
import com.object.ai.memory.service.AgentMemoryService;
import com.object.ai.memory.support.SessionPermissionChecker;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private static final String DEFAULT_SESSION_NAME = "新对话";

    @Resource
    private SessionMapper sessionMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private SessionPermissionChecker sessionPermissionChecker;

    @Override
    public List<SessionVO> findSessionList(SessionQueryDTO query) {
        String currentUserId = StpUtil.getLoginIdAsString();
        LambdaQueryWrapper<SessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionPO::getUserId, currentUserId);
        if (query != null) {
            wrapper.eq(StrUtil.isNotBlank(query.getAgentId()), SessionPO::getAgentId, query.getAgentId());
            wrapper.eq(StrUtil.isNotBlank(query.getStatus()), SessionPO::getStatus, query.getStatus());
            wrapper.like(StrUtil.isNotBlank(query.getSessionName()), SessionPO::getSessionName, query.getSessionName());
        }
        wrapper.orderByDesc(SessionPO::getLastMessageAt)
                .orderByDesc(SessionPO::getCreateTime);
        return sessionMapper.selectList(wrapper).stream()
                .map(this::toSessionListVO)
                .toList();
    }

    @Override
    public SessionVO createSession(SessionCreateDTO request) {
        if (request == null) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "创建会话参数不能为空");
        }

        String currentUserId = StpUtil.getLoginIdAsString();
        LocalDateTime now = LocalDateTime.now();
        String sessionId = StrUtil.blankToDefault(request.getId(), IdUtil.fastSimpleUUID());

        SessionPO existing = sessionMapper.selectById(sessionId);
        if (existing != null) {
            if (!StrUtil.equals(currentUserId, existing.getUserId())) {
                throw new BusinessException(BizErrorCode.NO_PERMISSION_ERROR, "会话 ID 已被占用");
            }
            return existing.toSessionVO();
        }

        SessionPO sessionPO = SessionPO.builder()
                .sessionName(StrUtil.blankToDefault(request.getSessionName(), DEFAULT_SESSION_NAME))
                .userId(currentUserId)
                .agentId(request.getAgentId())
                .summaryCount(0)
                .status(SessionStatusEnum.active.name())
                .build();
        sessionPO.setId(sessionId);
        sessionPO.setCreateBy(currentUserId);
        sessionPO.setUpdateBy(currentUserId);
        sessionPO.setCreateTime(now);
        sessionPO.setUpdateTime(now);
        sessionPO.setDeleted(false);

        int affectedRows = sessionMapper.insert(sessionPO);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "创建会话失败");
        }
        return sessionPO.toSessionVO();
    }

    @Override
    public Boolean updateSession(SessionUpdateDTO request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "会话 ID 不能为空");
        }
        if (StrUtil.isBlank(request.getSessionName())
                && StrUtil.isBlank(request.getAgentId())
                && StrUtil.isBlank(request.getStatus())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "至少提供一个待更新字段");
        }

        sessionPermissionChecker.checkOwner(request.getId());
        String currentUserId = StpUtil.getLoginIdAsString();

        SessionPO updateSession = new SessionPO();
        updateSession.setId(request.getId());
        if (StrUtil.isNotBlank(request.getSessionName())) {
            updateSession.setSessionName(request.getSessionName());
        }
        if (StrUtil.isNotBlank(request.getAgentId())) {
            updateSession.setAgentId(request.getAgentId());
        }
        if (StrUtil.isNotBlank(request.getStatus())) {
            updateSession.setStatus(parseStatus(request.getStatus()).name());
        }
        updateSession.setUpdateBy(currentUserId);
        updateSession.setUpdateTime(LocalDateTime.now());

        int affectedRows = sessionMapper.updateById(updateSession);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "更新会话失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSession(SessionDeleteDTO request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "会话 ID 不能为空");
        }

        sessionPermissionChecker.checkOwner(request.getId());

        int sessionAffectedRows = sessionMapper.deleteById(request.getId());
        if (sessionAffectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "删除会话失败");
        }

        messageMapper.physicalDeleteBySessionId(request.getId());
        return true;
    }

    @Override
    public List<MessageVO> findMessageList(MessageQueryDTO request) {
        if (request == null || StrUtil.isBlank(request.getSessionId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "会话 ID 不能为空");
        }

        sessionPermissionChecker.checkOwner(request.getSessionId());

        LambdaQueryWrapper<MessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessagePO::getSessionId, request.getSessionId())
                .orderByAsc(MessagePO::getMessageIndex);
        return messageMapper.selectList(wrapper).stream()
                .map(MessagePO::toMessageVO)
                .toList();
    }

    private SessionVO toSessionListVO(SessionPO sessionPO) {
        SessionVO sessionVO = sessionPO.toSessionVO();
        sessionVO.setLongTimeMemory(null);
        return sessionVO;
    }

    private SessionStatusEnum parseStatus(String status) {
        try {
            return SessionStatusEnum.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "会话状态不合法");
        }
    }
}
