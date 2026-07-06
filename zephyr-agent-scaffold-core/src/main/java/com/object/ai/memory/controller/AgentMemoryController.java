package com.object.ai.memory.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import com.object.ai.memory.model.request.MessageQueryDTO;
import com.object.ai.memory.model.request.SessionCreateDTO;
import com.object.ai.memory.model.request.SessionDeleteDTO;
import com.object.ai.memory.model.request.SessionQueryDTO;
import com.object.ai.memory.model.request.SessionUpdateDTO;
import com.object.ai.memory.model.vo.MessageVO;
import com.object.ai.memory.model.vo.SessionVO;
import com.object.ai.memory.service.AgentMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("agent/memory")
@Tag(name = "Agent消息记忆接口")
@RequiredArgsConstructor
@SaCheckLogin
public class AgentMemoryController {

    private final AgentMemoryService agentMemoryService;

    @Operation(summary = "查询会话列表")
    @PostMapping("findSessionList")
    public BaseResponse<List<SessionVO>> findSessionList(@RequestBody(required = false) SessionQueryDTO query) {
        return ResultUtil.ok(agentMemoryService.findSessionList(query));
    }

    @Operation(summary = "创建会话")
    @PostMapping("createSession")
    public BaseResponse<SessionVO> createSession(@RequestBody SessionCreateDTO request) {
        return ResultUtil.ok(agentMemoryService.createSession(request));
    }

    @Operation(summary = "更新会话")
    @PostMapping("updateSession")
    public BaseResponse<Boolean> updateSession(@RequestBody SessionUpdateDTO request) {
        return ResultUtil.ok(agentMemoryService.updateSession(request));
    }

    @Operation(summary = "删除会话")
    @PostMapping("deleteSession")
    public BaseResponse<Boolean> deleteSession(@RequestBody SessionDeleteDTO request) {
        return ResultUtil.ok(agentMemoryService.deleteSession(request));
    }

    @Operation(summary = "根据会话 ID 查询消息列表")
    @PostMapping("findMessageList")
    public BaseResponse<List<MessageVO>> findMessageList(@RequestBody MessageQueryDTO request) {
        return ResultUtil.ok(agentMemoryService.findMessageList(request));
    }
}
