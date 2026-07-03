package com.object.ai.agent.service.chat;

import com.object.ai.agent.model.valobj.AgentChatRequestVO;
import com.object.ai.agent.model.valobj.AgentStreamRequestVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent对话服务
 *
 * @author object
 */
public interface AgentChatService {

    /**
     * 同步对话
     *
     * @param agentChatRequestVO 对话请求参数
     * @return 对话结果
     */
    String chat(AgentChatRequestVO agentChatRequestVO);

    /**
     * 流式对话（SSE）
     *
     * @param request    对话请求参数
     * @param sseEmitter SSE发射器
     */
    void stream(AgentStreamRequestVO request, SseEmitter sseEmitter);

}
