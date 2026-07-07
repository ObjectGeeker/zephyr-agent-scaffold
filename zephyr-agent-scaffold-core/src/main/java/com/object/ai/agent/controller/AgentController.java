package com.object.ai.agent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.object.ai.agent.model.response.AgentInfoDTO;
import com.object.ai.agent.model.valobj.AgentChatRequestVO;
import com.object.ai.agent.model.valobj.AgentStreamRequestVO;
import com.object.ai.agent.service.AgentService;
import com.object.ai.agent.service.chat.AgentChatService;
import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent 对话接口
 *
 * @author object
 */
@Tag(name = "Agent对话", description = "Agent对话接口")
@RestController
@RequestMapping("/agent")
public class AgentController {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    @Resource
    private AgentChatService agentChatService;

    @Resource
    private AgentService agentService;

    @Operation(summary = "同步对话", description = "同步调用Agent并返回完整结果")
    @PostMapping("/chat")
    public String chat(@RequestBody AgentChatRequestVO agentChatRequestVO) {
        StpUtil.checkLogin();
        return agentChatService.chat(agentChatRequestVO);
    }

    /**
     * 流式对话接口（SSE）
     * <p>
     * 返回 Server-Sent Events 流，前端可通过 EventSource 或 fetch + ReadableStream 消费。
     * 每条事件数据为 AgentStreamResponseVO，包含 agentName、type、content 等字段。
     */
    @Operation(summary = "流式对话", description = "SSE流式返回Agent对话结果，支持工具调用与普通消息区分")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AgentStreamRequestVO request) {
        StpUtil.checkLogin();
        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT);
        agentChatService.stream(request, sseEmitter);
        return sseEmitter;
    }

    @Operation(summary = "查询当前配置的agent列表")
    @PostMapping("findAgentList")
    public BaseResponse<List<AgentInfoDTO>> findAgentList() {
        StpUtil.checkLogin();
        List<AgentInfoDTO> agentInfoDTOS = agentService.findAgentList();
        return ResultUtil.ok(agentInfoDTOS);
    }

}
