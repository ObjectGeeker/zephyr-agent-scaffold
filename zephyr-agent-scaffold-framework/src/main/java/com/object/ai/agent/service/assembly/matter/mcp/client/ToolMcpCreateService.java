package com.object.ai.agent.service.assembly.matter.mcp.client;

import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

public interface ToolMcpCreateService<T> {

    List<T> buildToolCallBack(AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp);

}
