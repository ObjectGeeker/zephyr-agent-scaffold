package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StdioServerParameters;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StdioToolMcpCreateService implements ToolMcpCreateService<McpToolset> {
    @Override
    public List<McpToolset> buildToolCallBack(AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.Agent.ToolMcp.StdioServerParameters stdio = toolMcp.getStdio();
        StdioServerParameters serverParams = StdioServerParameters.builder()
                .command(stdio.getServerParameters().getCommand())
                .args(stdio.getServerParameters().getArgs())
                .env(stdio.getServerParameters().getEnv())
                .build();
        return CollUtil.newArrayList(new McpToolset(serverParams.toServerParameters()));
    }
}
