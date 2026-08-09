package com.object.ai.agent.service.assembly.matter.mcp.client.factory;

import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.LocalToolMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.SseToolMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.StdioToolMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.StreamableHttpToolMcpCreateService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DefaultMcpClientFactory {

    @Resource
    private SseToolMcpCreateService sseToolMcpCreateService;

    @Resource
    private StdioToolMcpCreateService stdioToolMcpCreateService;

    @Resource
    private StreamableHttpToolMcpCreateService streamableHttpToolMcpCreateService;

    @Resource
    private LocalToolMcpCreateService localToolMcpCreateService;

    public ToolMcpCreateService<?> getToolMcpCreateService(AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp) {
        if (null != toolMcp.getFunctionCall()) return localToolMcpCreateService;
        if (null != toolMcp.getSse()) return sseToolMcpCreateService;
        if (null != toolMcp.getStreamableHttp()) return streamableHttpToolMcpCreateService;
        if (null != toolMcp.getStdio()) return stdioToolMcpCreateService;
        return null;
    }

}
