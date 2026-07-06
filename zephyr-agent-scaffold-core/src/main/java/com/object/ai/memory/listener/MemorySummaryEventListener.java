package com.object.ai.memory.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.object.ai.memory.event.MemorySummaryEvent;
import com.object.ai.memory.mapper.SessionMapper;
import com.object.ai.memory.model.po.SessionPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnBooleanProperty(prefix = "long-term-memory", value = "enabled")
public class MemorySummaryEventListener {

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            你是对话记忆总结助手。请根据以下对话内容，更新用户的长期记忆。
                        
            当前已有记忆：
            {existingMemory}
                        
            最新对话：
            {conversationText}
                        
            要求：
            1. 保留已有记忆中的关键信息
            2. 融合本次对话中的新信息
            3. 保持简洁，突出关键事实、用户偏好和重要上下文
            4. 输出纯文本，不超过500字
            5. 只需要输出总结后的内容，不需要掺咋其余任何多余话语！
            """;

    @Resource
    private SessionMapper sessionMapper;

    @Autowired(required = false)
    @Qualifier("longTermMemoryChatModel")
    private ChatModel longTermMemoryChatModel;

    @Async("memoryTaskExecutor")
    @EventListener
    public void onMemorySummary(MemorySummaryEvent event) {
        if (longTermMemoryChatModel == null) {
            return;
        }

        try {
            String threadId = event.getThreadId();
            SessionPO sessionPO = sessionMapper.selectById(threadId);
            if (sessionPO == null) {
                log.warn("长期记忆总结跳过，会话不存在，sessionId={}", threadId);
                return;
            }

            String conversationText = buildConversationText(event.getMessages());
            if (StrUtil.isBlank(conversationText)) {
                log.debug("长期记忆总结跳过，无有效对话内容，sessionId={}", threadId);
                return;
            }

            String existingMemory = StrUtil.blankToDefault(sessionPO.getLongTimeMemory(), "无");
            String prompt = SUMMARY_PROMPT_TEMPLATE
                    .replace("{existingMemory}", existingMemory)
                    .replace("{conversationText}", conversationText);

            String newMemory = longTermMemoryChatModel.call(new Prompt(new UserMessage(prompt)))
                    .getResult()
                    .getOutput()
                    .getText();
            if (StrUtil.isBlank(newMemory)) {
                log.warn("长期记忆总结结果为空，sessionId={}", threadId);
                return;
            }

            SessionPO updateSession = new SessionPO();
            updateSession.setId(threadId);
            updateSession.setLongTimeMemory(newMemory);
            updateSession.setSummaryCount((sessionPO.getSummaryCount() == null ? 0 : sessionPO.getSummaryCount()) + 1);
            updateSession.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(updateSession);
            log.info("长期记忆总结完成，sessionId={}, summaryCount={}", threadId, updateSession.getSummaryCount());
        } catch (Exception e) {
            log.error("长期记忆总结失败，sessionId={}", event.getThreadId(), e);
        }
    }

    private String buildConversationText(List<Message> messages) {
        if (CollUtil.isEmpty(messages)) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof SystemMessage) {
                continue;
            }
            if (message instanceof UserMessage userMessage) {
                if (StrUtil.isNotBlank(userMessage.getText())) {
                    lines.add("用户: " + userMessage.getText());
                }
            } else if (message instanceof AssistantMessage assistantMessage) {
                StringBuilder line = new StringBuilder("助手: ");
                if (StrUtil.isNotBlank(assistantMessage.getText())) {
                    line.append(assistantMessage.getText());
                }
                if (assistantMessage.hasToolCalls()) {
                    String toolNames = assistantMessage.getToolCalls().stream()
                            .map(AssistantMessage.ToolCall::name)
                            .collect(Collectors.joining(", "));
                    line.append(" [调用工具: ").append(toolNames).append("]");
                }
                if (!line.toString().equals("助手: ")) {
                    lines.add(line.toString());
                }
            } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                String toolText = joinToolResponses(toolResponseMessage);
                if (StrUtil.isNotBlank(toolText)) {
                    lines.add("工具: " + toolText);
                }
            }
        }
        return String.join("\n", lines);
    }

    private String joinToolResponses(ToolResponseMessage toolResponseMessage) {
        if (CollUtil.isEmpty(toolResponseMessage.getResponses())) {
            return null;
        }
        return toolResponseMessage.getResponses().stream()
                .map(ToolResponseMessage.ToolResponse::responseData)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n"));
    }
}
