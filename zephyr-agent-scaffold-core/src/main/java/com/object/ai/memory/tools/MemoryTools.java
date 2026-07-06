package com.object.ai.memory.tools;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.object.ai.memory.mapper.MessageMapper;
import com.object.ai.memory.mapper.SessionMapper;
import com.object.ai.memory.model.po.MessagePO;
import com.object.ai.memory.model.po.SessionPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 会话记忆查询工具：在 Redis 短期 checkpoint 过期或上下文窗口裁剪后，按需从 MySQL 拉取历史消息与长期记忆。
 */
@Component
@Slf4j
public class MemoryTools {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private SessionMapper sessionMapper;

    @Tool(description = """
            获取当前会话的历史消息记录。当用户询问过往对话内容、引用之前讨论的细节、
            或当前上下文不足以回答时使用。返回按时间顺序排列的消息，支持翻页。
            """)
    public String getConversationHistory(
            @ToolParam(description = "返回条数，默认 20，最大 50", required = false) Integer limit,
            @ToolParam(description = "只返回该消息序号之前的记录，用于向更早的消息翻页", required = false) Integer beforeIndex,
            ToolContext toolContext) {
        Optional<String> sessionIdOpt = MemoryToolSupport.resolveSessionId(toolContext);
        if (sessionIdOpt.isEmpty()) {
            return "无法识别当前会话，请稍后重试。";
        }
        String sessionId = sessionIdOpt.get();
        if (!canAccessSession(sessionId, toolContext)) {
            return "无权限访问该会话的历史消息。";
        }

        int queryLimit = MemoryToolSupport.normalizeLimit(limit);
        List<MessagePO> messages = messageMapper.selectHistoryMessages(sessionId, beforeIndex, queryLimit);
        long totalCount = countMessages(sessionId);

        Integer oldestIndex = messages.isEmpty() ? null : messages.get(0).getMessageIndex();
        Integer newestIndex = messages.isEmpty() ? null : messages.get(messages.size() - 1).getMessageIndex();
        return MemoryToolSupport.formatHistory(messages, oldestIndex, newestIndex, (int) totalCount);
    }

    @Tool(description = """
            获取当前会话的长期记忆摘要。当需要了解用户偏好、关键事实或跨轮次上下文，
            且系统提示中未包含足够信息时使用。
            """)
    public String getLongTermMemory(ToolContext toolContext) {
        Optional<String> sessionIdOpt = MemoryToolSupport.resolveSessionId(toolContext);
        if (sessionIdOpt.isEmpty()) {
            return "无法识别当前会话，请稍后重试。";
        }
        String sessionId = sessionIdOpt.get();
        if (!canAccessSession(sessionId, toolContext)) {
            return "无权限访问该会话的长期记忆。";
        }

        SessionPO sessionPO = sessionMapper.selectById(sessionId);
        if (sessionPO == null) {
            return "会话不存在。";
        }
        if (StrUtil.isBlank(sessionPO.getLongTimeMemory())) {
            return "当前会话尚无长期记忆摘要。";
        }
        return sessionPO.getLongTimeMemory();
    }

    private boolean canAccessSession(String sessionId, ToolContext toolContext) {
        SessionPO sessionPO = sessionMapper.selectById(sessionId);
        if (sessionPO == null) {
            return false;
        }
        Optional<String> userIdOpt = MemoryToolSupport.resolveUserId(toolContext);
        if (userIdOpt.isEmpty()) {
            log.warn("记忆 Tool 缺少 user_id 元数据，拒绝访问 sessionId={}", sessionId);
            return false;
        }
        return StrUtil.equals(userIdOpt.get(), sessionPO.getUserId());
    }

    private long countMessages(String sessionId) {
        LambdaQueryWrapper<MessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessagePO::getSessionId, sessionId);
        return messageMapper.selectCount(wrapper);
    }
}
