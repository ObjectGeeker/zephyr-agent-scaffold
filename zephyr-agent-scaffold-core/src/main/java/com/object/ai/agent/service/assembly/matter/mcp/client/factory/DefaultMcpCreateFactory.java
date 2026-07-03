package com.object.ai.agent.service.assembly.matter.mcp.client.factory;

import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.LocalMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.SseMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.impl.StdioMcpCreateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DefaultMcpCreateFactory {

    @Resource
    private SseMcpCreateService sseMcpCreateService;

    @Resource
    private StdioMcpCreateService stdioMcpCreateService;

    @Resource
    private LocalMcpCreateService localMcpCreateService;

    public ToolMcpCreateService getToolMcpCreateService(AgentConfigTableVO.Module.AgentNode.ToolMcp toolMcp) {
        if (null != toolMcp.getLocal()) return localMcpCreateService;
        if (null != toolMcp.getSse()) return sseMcpCreateService;
        if (null != toolMcp.getStdio()) return stdioMcpCreateService;
        return null;
    }

}
