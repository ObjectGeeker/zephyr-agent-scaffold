package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

@Component
@Slf4j
public class SseMcpCreateService implements ToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallbacks(AgentConfigTableVO.Module.AgentNode.ToolMcp toolMcp) throws MalformedURLException {
        AgentConfigTableVO.Module.AgentNode.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();

        String originalBaseUri = sseConfig.getBaseUri();
        String sseEndpoint = sseConfig.getSseEndpoint();
        String baseUrl = parseBaseUrl(originalBaseUri, sseEndpoint);
        sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(baseUrl)
                .sseEndpoint(sseEndpoint)
                .build();

        McpSyncClient mcpSyncClient = McpClient
                .sync(sseClientTransport)
                .requestTimeout(Duration.ofMillis(sseConfig.getRequestTimeout())).build();
        McpSchema.InitializeResult initialize = mcpSyncClient.initialize();

        log.info("tool sse mcp initialize {}", initialize);
        return SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }

    private String parseBaseUrl(String originalBaseUri, String sseEndpoint) throws MalformedURLException {
        String baseUri = originalBaseUri;

        if (StringUtils.isBlank(sseEndpoint)) {
            URL url = new URL(originalBaseUri);

            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();

            String baseUrl = port == -1 ? protocol + "://" + host : protocol + "://" + host + ":" + port;

            int index = originalBaseUri.indexOf(baseUrl);
            if (index != -1) {
                sseEndpoint = originalBaseUri.substring(index + baseUrl.length());
            }

            baseUri = baseUrl;
        }

        return baseUri;
    }
}
