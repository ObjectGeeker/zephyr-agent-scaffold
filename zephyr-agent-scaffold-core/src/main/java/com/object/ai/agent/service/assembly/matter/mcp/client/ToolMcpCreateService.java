package com.object.ai.agent.service.assembly.matter.mcp.client;

import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import org.springframework.ai.tool.ToolCallback;

import java.net.MalformedURLException;

public interface ToolMcpCreateService {

    /**
     * 获取Tools(绑定ChatModel)
     *
     * @param toolMcp 配置的mcp
     * @return tools
     */
    ToolCallback[] buildToolCallbacks(AgentConfigTableVO.Module.AgentNode.ToolMcp toolMcp) throws MalformedURLException;

}
