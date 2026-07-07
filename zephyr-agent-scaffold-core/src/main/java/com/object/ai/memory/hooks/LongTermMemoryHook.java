package com.object.ai.memory.hooks;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.object.ai.memory.constants.MemoryMetadataKeys;
import com.object.ai.memory.event.MemorySummaryEvent;
import com.object.ai.memory.mapper.MessageMapper;
import com.object.ai.memory.mapper.SessionMapper;
import com.object.ai.memory.model.enums.MessageRoleEnum;
import com.object.ai.memory.model.po.MessagePO;
import com.object.ai.memory.model.po.SessionPO;
import com.object.ai.memory.model.vo.MessageVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@HookPositions(value = {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class LongTermMemoryHook extends MessagesModelHook {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private SessionMapper sessionMapper;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public String getName() {
        return "long_term_memory_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        if (shouldSkipMessageSave(config)) {
            return new AgentCommand(messages);
        }
        Optional<String> sessionId = config.threadId();
        if (sessionId.isEmpty()) {
            return new AgentCommand(messages);
        }

        // 填充长期记忆到systemMessage
        fillLongTermMemory(messages, config);

        Optional<UserMessage> userMessageOpt = findLatestUserMessageInTail(messages, 2);
        if (userMessageOpt.isEmpty()) {
            return new AgentCommand(messages);
        }
        UserMessage userMessage = userMessageOpt.get();

        List<String> fileIds = config.metadata(MemoryMetadataKeys.FILE_IDS, new TypeRef<List<String>>() {
        }).orElse(null);
        int messageIndex = resolveNextMessageIndex(sessionId.get());
        toMessageVO(userMessage, sessionId.get(), messageIndex, fileIds)
                .ifPresent(this::persistMessage);

        return new AgentCommand(messages);
    }

    private void fillLongTermMemory(List<Message> messages, RunnableConfig config) {
        String sessionId = config.threadId().orElse(null);
        SessionPO sessionPO = sessionMapper.selectById(sessionId);
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof SystemMessage sysMsg) {
                if (sessionPO != null && StrUtil.isNotBlank(sessionPO.getLongTimeMemory())) {
                    String enhanced = sysMsg.getText()
                            + "\n\n---\n以下是用户长期记忆，请在回答时参考：\n"
                            + sessionPO.getLongTimeMemory();
                    messages.set(i, new SystemMessage(enhanced));
                    break;
                }
            }
        }
    }

    @Override
    public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
        if (shouldSkipMessageSave(config)) {
            return new AgentCommand(previousMessages);
        }
        Optional<String> sessionId = config.threadId();
        if (sessionId.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        // 仅轮次结束（最终 Assistant 回复、无 toolCalls）时发布总结事件
        if (shouldPublishSummaryEvent(previousMessages)) {
            applicationEventPublisher.publishEvent(new MemorySummaryEvent(sessionId.get(), previousMessages));
        }

        Message lastMessage = CollUtil.getLast(previousMessages);
        if (lastMessage instanceof SystemMessage) {
            return new AgentCommand(previousMessages);
        }

        int messageIndex = resolveNextMessageIndex(sessionId.get());
        toMessageVO(lastMessage, sessionId.get(), messageIndex, null)
                .ifPresent(this::persistMessage);
        return new AgentCommand(previousMessages);
    }

    private boolean shouldSkipMessageSave(RunnableConfig config) {
        return !config.metadata(MemoryMetadataKeys.SAVE_MESSAGE, new TypeRef<Boolean>() {
        }).orElse(false);
    }

    /**
     * 工具循环中 afterModel 会多次触发；仅在最终 Assistant 回复（无 toolCalls）时总结。
     */
    private boolean shouldPublishSummaryEvent(List<Message> messages) {
        if (CollUtil.isEmpty(messages)) {
            return false;
        }
        Message lastMessage = CollUtil.getLast(messages);
        if (!(lastMessage instanceof AssistantMessage assistantMessage)) {
            return false;
        }
        return !assistantMessage.hasToolCalls();
    }

    private int resolveNextMessageIndex(String sessionId) {
        Integer maxIndex = messageMapper.selectMaxMessageIndex(sessionId);
        return maxIndex == null ? 0 : maxIndex + 1;
    }

    /**
     * 从消息列表底部向上扫描最近 tailSize 条，返回第一个 UserMessage。
     * 配置了 instruction 时末尾可能是 AgentInstructionMessage，真实用户消息在倒数第二条。
     */
    private Optional<UserMessage> findLatestUserMessageInTail(List<Message> messages, int tailSize) {
        if (CollUtil.isEmpty(messages)) {
            return Optional.empty();
        }
        int start = Math.max(0, messages.size() - tailSize);
        for (int i = messages.size() - 1; i >= start; i--) {
            Message message = messages.get(i);
            if (message instanceof UserMessage userMessage) {
                return Optional.of(userMessage);
            }
        }
        return Optional.empty();
    }

    private Optional<MessageVO> toMessageVO(Message message, String sessionId, int messageIndex,
                                            List<String> fileIds) {
        if (message instanceof SystemMessage) {
            return Optional.empty();
        }

        MessageVO.MessageVOBuilder builder = MessageVO.builder()
                .sessionId(sessionId)
                .messageIndex(messageIndex);

        if (message instanceof UserMessage userMessage) {
            builder.role(MessageRoleEnum.user)
                    .messageContent(userMessage.getText())
                    .attachment(CollUtil.isEmpty(fileIds) ? null : fileIds);
        } else if (message instanceof AssistantMessage assistantMessage) {
            builder.role(MessageRoleEnum.assistant)
                    .messageContent(assistantMessage.getText());
            if (assistantMessage.hasToolCalls()) {
                builder.metadata(buildToolCallsMetadata(assistantMessage.getToolCalls()));
            }
        } else if (message instanceof ToolResponseMessage toolResponseMessage) {
            builder.role(MessageRoleEnum.tool)
                    .messageContent(joinToolResponses(toolResponseMessage))
                    .metadata(buildToolResponseMetadata(toolResponseMessage));
        } else {
            return Optional.empty();
        }

        return Optional.of(builder.build());
    }

    private Map<String, Object> buildToolCallsMetadata(List<AssistantMessage.ToolCall> toolCalls) {
        List<Map<String, Object>> items = new ArrayList<>(toolCalls.size());
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            Map<String, Object> item = new HashMap<>();
            item.put("toolCallId", toolCall.id());
            item.put("toolCallName", toolCall.name());
            item.put("arguments", toolCall.arguments());
            items.add(item);
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("toolCalls", items);
        return metadata;
    }

    private Map<String, Object> buildToolResponseMetadata(ToolResponseMessage toolResponseMessage) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (CollUtil.isNotEmpty(toolResponseMessage.getResponses())) {
            for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                Map<String, Object> item = new HashMap<>();
                item.put("toolCallId", response.id());
                item.put("toolCallName", response.name());
                item.put("responseData", response.responseData());
                items.add(item);
            }
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("responses", items);
        return metadata;
    }

    private String joinToolResponses(ToolResponseMessage toolResponseMessage) {
        if (CollUtil.isEmpty(toolResponseMessage.getResponses())) {
            return null;
        }
        return toolResponseMessage.getResponses().stream()
                .map(ToolResponseMessage.ToolResponse::responseData)
                .collect(Collectors.joining("\n"));
    }

    private void persistMessage(MessageVO messageVO) {
        try {
            MessagePO messagePO = messageVO.toMessagePO();
            messagePO.setDeleted(false);
            messageMapper.upsertBySessionAndIndex(messagePO);
            updateSessionLastMessageAt(messageVO.getSessionId());
        } catch (Exception e) {
            log.error("消息落库失败，sessionId={}, messageIndex={}",
                    messageVO.getSessionId(), messageVO.getMessageIndex(), e);
        }
    }

    private void updateSessionLastMessageAt(String sessionId) {
        SessionPO existing = sessionMapper.selectById(sessionId);
        if (existing == null) {
            log.warn("会话不存在，跳过更新 last_message_at，sessionId={}", sessionId);
            return;
        }
        SessionPO patch = new SessionPO();
        patch.setId(sessionId);
        patch.setLastMessageAt(LocalDateTime.now());
        sessionMapper.updateById(patch);
    }
}
