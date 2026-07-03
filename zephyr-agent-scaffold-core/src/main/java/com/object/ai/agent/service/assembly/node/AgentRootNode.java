package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 根节点
 *
 * @author object
 */
@Component
public class AgentRootNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentApiNode agentApiNode;

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        if (requestParameter.getConfig() == null) {
            log.info("agent auto config is null, return");
            return null;
        }
        log.info("agent auto assembly start, agent type {}", requestParameter.getConfig().getAppType());
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return agentApiNode;
    }
}
