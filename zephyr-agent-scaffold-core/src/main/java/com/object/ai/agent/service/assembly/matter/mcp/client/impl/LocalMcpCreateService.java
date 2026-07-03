package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;

@Component
@Slf4j
public class LocalMcpCreateService implements ToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallbacks(AgentConfigTableVO.Module.AgentNode.ToolMcp toolMcp) throws MalformedURLException {
        AgentConfigTableVO.Module.AgentNode.ToolMcp.ToolFunctionCall localConfig = toolMcp.getLocal();
        try {
            Object toolBean = SpringUtil.getBean(localConfig.getBeanName());
            return ToolCallbacks.from(toolBean);
        } catch (BeansException e) {
            log.error("【Agent自动装配】McpToolset创建失败", e);
        }
        return new ToolCallback[0];
    }
}
