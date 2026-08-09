package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class StreamableHttpToolMcpCreateService implements ToolMcpCreateService<McpToolset> {
    @Override
    public List<McpToolset> buildToolCallBack(AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.Agent.ToolMcp.StreamableHttpServerParameters httpConfig =
                toolMcp.getStreamableHttp();

        StreamableHttpServerParameters serverParameters = StreamableHttpServerParameters.builder()
                .url(httpConfig.getUrl())
                .headers(nullToEmpty(httpConfig.getHeaders()))
                .timeout(Duration.ofSeconds(httpConfig.getRequestTimeout()))
                .readTimeout(Duration.ofSeconds(httpConfig.getReadTimeout()))
                .terminateOnClose(Boolean.TRUE.equals(httpConfig.getTerminateOnClose()))
                .build();
        return CollUtil.newArrayList(new McpToolset(serverParameters));
    }

    private Map<String, String> nullToEmpty(Map<String, String> headers) {
        return headers == null ? Map.of() : headers;
    }
}
