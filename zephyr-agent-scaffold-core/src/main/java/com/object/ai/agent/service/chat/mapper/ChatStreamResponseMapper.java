package com.object.ai.agent.service.chat.mapper;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.object.ai.agent.model.valobj.AgentStreamResponseVO;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.Optional;
import java.util.stream.Collectors;

public class ChatStreamResponseMapper {
    public static final String AGENT_COMPLETE = "AGENT_COMPLETE";

    public static final String ASSISTANT_TYPE = "assistant";

    public static final String TOOL_TYPE = "tool";

    public Optional<AgentStreamResponseVO> from(NodeOutput nodeOutput) {
        if (!(nodeOutput instanceof StreamingOutput<?> streamingOutput)) {
            return Optional.empty();
        }

        OutputType outputType = streamingOutput.getOutputType();
        AgentStreamResponseVO responseDTO = AgentStreamResponseVO.builder()
                .agentName(nodeOutput.agent()).build();

        switch (outputType) {
            case AGENT_MODEL_STREAMING -> {
                processModelStreaming(streamingOutput, responseDTO);
            }
            case AGENT_MODEL_FINISHED -> {
                processModelFinished(streamingOutput, responseDTO);
            }
            case AGENT_TOOL_STREAMING -> {
                processToolStreaming(streamingOutput, responseDTO);
            }
            case AGENT_TOOL_FINISHED -> {
                processToolFinished(streamingOutput, responseDTO);
            }
        }

        return Optional.of(responseDTO);
    }

    protected void processToolFinished(StreamingOutput<?> streamingOutput, AgentStreamResponseVO responseDTO) {
        Message message = streamingOutput.message();
        responseDTO.setType(TOOL_TYPE);
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            if (CollUtil.isNotEmpty(toolResponseMessage.getResponses())) {
                ToolResponseMessage.ToolResponse first = toolResponseMessage.getResponses().get(0);
                responseDTO.setToolCallName(first.name());
                responseDTO.setToolCallId(first.id());
                responseDTO.setToolCallResponse(toolResponseMessage.getResponses().stream()
                        .map(ToolResponseMessage.ToolResponse::responseData)
                        .collect(Collectors.joining("\n")));
                return;
            }
        }
        if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
            AssistantMessage.ToolCall toolCall = assistantMessage.getToolCalls().get(0);
            responseDTO.setToolCallId(toolCall.id());
            responseDTO.setToolCallName(toolCall.name());
        }
    }

    protected void processToolStreaming(StreamingOutput<?> streamingOutput, AgentStreamResponseVO responseDTO) {
        processModelFinished(streamingOutput, responseDTO);
    }

    protected void processModelFinished(StreamingOutput<?> streamingOutput, AgentStreamResponseVO responseDTO) {
        processModelStreaming(streamingOutput, responseDTO);
    }

    protected void processModelStreaming(StreamingOutput<?> nodeOutput, AgentStreamResponseVO responseDTO) {
        AssistantMessage message = (AssistantMessage) nodeOutput.message();
        if (message == null) {
            return;
        }
        // 处理工具调用
        if (message.hasToolCalls()) {
            AssistantMessage.ToolCall toolCall = message.getToolCalls().get(0);
            responseDTO.setType(TOOL_TYPE);
            responseDTO.setToolCallId(toolCall.id());
            responseDTO.setToolCallName(toolCall.name());
            return;
        }
        // 处理正常消息
        responseDTO.setType(ASSISTANT_TYPE);
        responseDTO.setContent(message.getText());
    }

    public AgentStreamResponseVO complete() {
        return AgentStreamResponseVO.builder()
                .type(AGENT_COMPLETE)
                .build();
    }
}
