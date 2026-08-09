package com.object.ai.agent.service;

import com.object.ai.agent.model.request.AgentSessionCreateRequest;
import com.object.ai.agent.model.request.AgentChatRequest;
import com.object.ai.agent.model.response.AgentSessionCreateResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AgentChatService {

    /**
     * 创建一个新的 Agent 会话。
     */
    AgentSessionCreateResponse createSession(AgentSessionCreateRequest request);

    /**
     * 同步对话接口
     *
     * @param agentChatRequest 对话请求体
     * @return 回复消息
     */
    List<String> chat(AgentChatRequest agentChatRequest);

    /**
     * 流式对话接口
     *
     * @param agentChatRequest 对话请求体
     * @param sseEmitter sse
     */
    void stream(AgentChatRequest agentChatRequest, SseEmitter sseEmitter);
}
