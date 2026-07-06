package com.object.ai.agent.service.chat.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.object.ai.agent.model.context.ModelContextHolder;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.model.valobj.AgentChatRequestVO;
import com.object.ai.agent.model.valobj.AgentStreamRequestVO;
import com.object.ai.agent.model.valobj.AgentStreamResponseVO;
import com.object.ai.agent.service.chat.AgentChatService;
import com.object.ai.agent.service.chat.mapper.ChatStreamResponseMapper;
import com.object.ai.file.service.MultiModalMediaService;
import com.object.ai.memory.constants.MemoryMetadataKeys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@Slf4j
public class AgentChatServiceImpl implements AgentChatService {

    private final ChatStreamResponseMapper chatStreamResponseMapper = new ChatStreamResponseMapper();

    @Resource
    private MultiModalMediaService multiModalMediaService;

    @Override
    public String chat(AgentChatRequestVO agentChatRequestVO) {
        String agentId = agentChatRequestVO.getAgentId();
        AgentAssemblyRegisterVO registerVO = SpringUtil.getBean(
                "Agent_" + agentId, AgentAssemblyRegisterVO.class);
        String threadId = agentChatRequestVO.getUserId() + "_" + agentChatRequestVO.getSessionId();
        RunnableConfig config = buildMessageSaveConfig(threadId, agentChatRequestVO.getFileIds(),
                agentChatRequestVO.getSaveMessage());
        try {
            UserMessage userMessage = buildUserMessage(agentChatRequestVO.getUserMessage(),
                    agentChatRequestVO.getFileIds());
            return registerVO.getRunnerAgent()
                    .invokeAndGetOutput(userMessage, config)
                    .map(this::extractAssistantText)
                    .orElse(null);
        } catch (Exception e) {
            log.error("agent chat error", e);
            return null;
        }
    }

    @Override
    public void stream(AgentStreamRequestVO request, SseEmitter sseEmitter) {
        streamChat(request, sseEmitter,
                chatStreamResponseMapper::from,
                chatStreamResponseMapper::complete);
    }

    private void streamChat(
            AgentStreamRequestVO chatRequestDTO,
            SseEmitter sseEmitter,
            Function<NodeOutput, Optional<AgentStreamResponseVO>> nodeMapper,
            Supplier<AgentStreamResponseVO> completeSupplier) {
        String agentId = chatRequestDTO.getAgentId();
        AgentAssemblyRegisterVO registerVO = SpringUtil.getBean(
                "Agent_" + agentId, AgentAssemblyRegisterVO.class);
        String threadId = chatRequestDTO.getThreadId();
        RunnableConfig config = buildMessageSaveConfig(threadId, chatRequestDTO.getFileIds(),
                chatRequestDTO.getSaveMessage());
        try {
            UserMessage userMessage = buildUserMessage(chatRequestDTO.getMessage(), chatRequestDTO.getFileIds());
            Flux<NodeOutput> flux = registerVO.getRunnerAgent()
                    .stream(userMessage, config);
            flux.subscribe(
                    nodeOutput -> nodeMapper.apply(nodeOutput)
                            .ifPresent(response -> sendStreamEvent(sseEmitter, response)),
                    sseEmitter::completeWithError,
                    () -> {
                        sendStreamEvent(sseEmitter, completeSupplier.get());
                        sseEmitter.complete();
                    }
            );
        } catch (Exception e) {
            log.error("agent chat error", e);
            sseEmitter.completeWithError(e);
        }
    }

    /**
     * 构建对话用户消息，按需附加多模态内容。
     */
    private UserMessage buildUserMessage(String text, List<String> fileIds) {
        List<Media> mediaList = multiModalMediaService.loadMediaByFileIds(fileIds);
        UserMessage.Builder builder = UserMessage.builder().text(text);
        if (CollUtil.isNotEmpty(mediaList)) {
            builder.media(mediaList);
            ModelContextHolder.get().setMultiModel(true);
        }
        return builder.build();
    }

    private RunnableConfig buildMessageSaveConfig(String threadId, List<String> fileIds, Boolean saveMessage) {
        RunnableConfig.Builder builder = RunnableConfig.builder()
                .threadId(threadId);
        if (Boolean.TRUE.equals(saveMessage)) {
            builder.addMetadata(MemoryMetadataKeys.SAVE_MESSAGE, true);
            if (CollUtil.isNotEmpty(fileIds)) {
                builder.addMetadata(MemoryMetadataKeys.FILE_IDS, fileIds);
            }
        }
        return builder.build();
    }

    private void sendStreamEvent(SseEmitter sseEmitter, AgentStreamResponseVO response) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .name("message")
                    .data(response, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            sseEmitter.completeWithError(e);
        }
    }

    /**
     * 解析AI返回的消息（原有同步对话使用）
     */
    private String extractAssistantText(NodeOutput state) {
        Map<String, Object> dataObject = state.state().data();
        ArrayList<Object> messages = MapUtil.get(dataObject, "messages", ArrayList.class);
        AssistantMessage assistantMessage = (AssistantMessage) messages.get(messages.size() - 1);
        return assistantMessage.getText();
    }

}
