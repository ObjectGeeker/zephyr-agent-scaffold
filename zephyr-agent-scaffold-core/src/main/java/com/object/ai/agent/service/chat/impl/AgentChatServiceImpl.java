package com.object.ai.agent.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.object.ai.agent.constants.ModelMetadataKeys;
import com.object.ai.agent.model.context.ModelContextHolder;
import com.object.ai.agent.model.context.ModelCredentials;
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
        RunnableConfig config = buildRunnableConfig(threadId, agentChatRequestVO.getFileIds(),
                agentChatRequestVO.getSaveMessage(), null);
        try {
            UserMessage userMessage = buildUserMessage(agentChatRequestVO.getUserMessage(),
                    agentChatRequestVO.getFileIds(), null);
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
        ModelCredentials credentials = buildModelCredentials(chatRequestDTO);
        RunnableConfig config = buildRunnableConfig(threadId, chatRequestDTO.getFileIds(),
                chatRequestDTO.getSaveMessage(), credentials);
        try {
            UserMessage userMessage = buildUserMessage(chatRequestDTO.getMessage(), chatRequestDTO.getFileIds(),
                    credentials);
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

    private ModelCredentials buildModelCredentials(AgentStreamRequestVO request) {
        return ModelCredentials.builder()
                .apiKey(request.getApiKey())
                .model(request.getModel())
                .baseUrl(request.getBaseUrl())
                .completionsPath(request.getCompletionPath())
                .multiModel(false)
                .build();
    }

    private boolean hasByokFields(ModelCredentials credentials) {
        if (credentials == null) {
            return false;
        }
        return StrUtil.isNotBlank(credentials.getApiKey())
                || StrUtil.isNotBlank(credentials.getModel())
                || StrUtil.isNotBlank(credentials.getBaseUrl())
                || StrUtil.isNotBlank(credentials.getCompletionsPath());
    }

    /**
     * 构建对话用户消息，按需附加多模态内容。
     */
    private UserMessage buildUserMessage(String text, List<String> fileIds, ModelCredentials credentials) {
        List<Media> mediaList = multiModalMediaService.loadMediaByFileIds(fileIds);
        UserMessage.Builder builder = UserMessage.builder().text(text);
        if (CollUtil.isNotEmpty(mediaList)) {
            builder.media(mediaList);
            if (credentials != null) {
                credentials.setMultiModel(true);
            }
        }
        return builder.build();
    }

    private RunnableConfig buildRunnableConfig(String threadId, List<String> fileIds, Boolean saveMessage,
                                               ModelCredentials credentials) {
        RunnableConfig.Builder builder = RunnableConfig.builder()
                .threadId(threadId);
        if (StpUtil.isLogin()) {
            builder.addMetadata(MemoryMetadataKeys.USER_ID, StpUtil.getLoginIdAsString());
        }
        if (Boolean.TRUE.equals(saveMessage)) {
            builder.addMetadata(MemoryMetadataKeys.SAVE_MESSAGE, true);
            if (CollUtil.isNotEmpty(fileIds)) {
                builder.addMetadata(MemoryMetadataKeys.FILE_IDS, fileIds);
            }
        }
        if (hasByokFields(credentials)) {
            builder.addMetadata(ModelMetadataKeys.MODEL_CREDENTIALS, credentials);
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
