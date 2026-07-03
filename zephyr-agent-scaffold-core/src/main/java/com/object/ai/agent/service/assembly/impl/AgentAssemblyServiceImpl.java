package com.object.ai.agent.service.assembly.impl;

import com.object.ai.agent.model.properties.AgentAutoConfigProperties;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.AgentAssemblyService;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@Slf4j
public class AgentAssemblyServiceImpl implements AgentAssemblyService {

    @Resource
    private DefaultAgentAssemblyFactory defaultAgentAssemblyFactory;

    @Override
    public void assembly(AgentAutoConfigProperties properties) {
        if (properties.isEnabled()) {
            AbstractAgentAssemblySupport rootNode = defaultAgentAssemblyFactory.getRootNode();
            Collection<AgentConfigTableVO> configTableList = properties.getTableMap().values();
            for (AgentConfigTableVO agentConfigTableVO : configTableList) {
                try {
                    log.info("agent auto armory app name {}", agentConfigTableVO.getAppName());
                    rootNode.apply(AgentAssemblyCommandVO.builder().config(agentConfigTableVO).build(), new DefaultAgentAssemblyFactory.DynamicContext());
                } catch (Exception e) {
                    log.error("agent auto armory error", e);
                }
            }
        }
    }
}
