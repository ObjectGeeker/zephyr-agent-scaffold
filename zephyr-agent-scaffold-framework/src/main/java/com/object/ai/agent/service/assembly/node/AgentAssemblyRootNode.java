package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AgentAssemblyRootNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentAssemblyChatModelNode chatModelNode;

    @Override
    protected AiAgentRegisterVO doApply(AgentAssemblyCommandEntity requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> get(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return chatModelNode;
    }

}
