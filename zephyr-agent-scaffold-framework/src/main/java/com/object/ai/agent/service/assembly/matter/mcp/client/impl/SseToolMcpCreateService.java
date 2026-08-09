package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.SseServerParameters;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class SseToolMcpCreateService implements ToolMcpCreateService<McpToolset> {
    @Override
    public List<McpToolset> buildToolCallBack(AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.Agent.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        // https://127.0.0.1:9999/sse?apikey=DElk89iu8Ehhnbu
        String originalBaseUri = sseConfig.getBaseUri();
        String baseUri;
        String sseEndpoint;

        int queryParamStartIndex = originalBaseUri.indexOf("sse");
        if (queryParamStartIndex != -1) {
            baseUri = originalBaseUri.substring(0, queryParamStartIndex - 1);
            sseEndpoint = originalBaseUri.substring(queryParamStartIndex - 1);
        } else {
            baseUri = originalBaseUri;
            sseEndpoint = sseConfig.getSseEndpoint();
        }

        sseEndpoint = StrUtil.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

        SseServerParameters sseServerParameters = SseServerParameters.builder()
                .url(baseUri)
                .sseEndpoint(sseEndpoint)
                .timeout(Duration.ofSeconds(sseConfig.getRequestTimeout()))
                .build();
        return CollUtil.newArrayList(new McpToolset(sseServerParameters));
    }
}
