package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class StdioMcpCreateService implements ToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallbacks(AgentConfigTableVO.Module.AgentNode.ToolMcp toolMcp) {
        AgentConfigTableVO.Module.AgentNode.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();
        AgentConfigTableVO.Module.AgentNode.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();

        ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
                .args(serverParameters.getArgs())
                .env(serverParameters.getEnv())
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new JsonMapper())))
                .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();

        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

        log.info("tool stdio mcp initialize {}", initialize);
        return SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }
}
