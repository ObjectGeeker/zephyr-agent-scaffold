package com.object.ai.agent.controller;

import cn.hutool.core.collection.CollUtil;
import com.object.ai.agent.model.request.AgentChatRequest;
import com.object.ai.agent.model.request.AgentSessionCreateRequest;
import com.object.ai.agent.model.response.AiAgentInfoDTO;
import com.object.ai.agent.model.response.AgentSessionCreateResponse;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import com.object.ai.agent.service.AgentChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("agent")
public class AgentController {

    @Resource
    private AgentChatService agentChatService;

    @Resource
    private AiAgentAutoConfigProperties properties;

    @PostMapping("query_ai_agent_list")
    public List<AiAgentInfoDTO> queryAiAgentList() {
        Map<String, AiAgentConfigTableVO> tableMap = properties.getTableMap();
        if (CollUtil.isEmpty(tableMap)) {
            return CollUtil.newArrayList();
        }
        return tableMap.values().stream().map(AiAgentInfoDTO::toAiAgentInfo).toList();
    }

    @PostMapping("session/create")
    public AgentSessionCreateResponse createSession(@RequestBody AgentSessionCreateRequest request) {
        return agentChatService.createSession(request);
    }

    @PostMapping("chat")
    public List<String> chat(@RequestBody AgentChatRequest agentChatRequest) {
        return agentChatService.chat(agentChatRequest);
    }

    @PostMapping("stream")
    public SseEmitter stream(@RequestBody AgentChatRequest agentChatRequest) {
        // SseEmitter 是 Spring MVC 提供的“异步响应容器”。
        // Controller 不会在这里等待 AI 完整回答，而是先把这个容器交给 Spring，
        // 后续 Agent 每产生一个 Event，Service 就通过它向同一个 HTTP 连接发送一条 SSE。
        SseEmitter sseEmitter = new SseEmitter(3000000L);

        // 这个超时时间单位是毫秒。它表示连接最长可以保持多久，
        // 并不代表 AI 一定要在这么长时间内返回完整答案。
        // Service 会在这里注册 Flowable 的订阅、发送逻辑和生命周期回调。
        agentChatService.stream(agentChatRequest, sseEmitter);

        // 返回 emitter 后，Spring MVC 才会把请求交给异步响应流程；
        // 后续 sseEmitter.send(...) 的内容会持续写入客户端。
        return sseEmitter;
    }

}
