package com.object.ai.agent.service.assembly.node.multagent;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.node.MultiAgentNode;
import org.springframework.stereotype.Component;

@Component
public class LoopAgentNode extends AbstractAgentAssemblySupport {

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly loop agent node start");

        buildMultiAgent(dynamicContext, (currentAgent, subAgents) -> LoopAgent.builder()
                .name(currentAgent.getName())
                .description(currentAgent.getDescription())
                .subAgents(subAgents)
                .build());

        dynamicContext.getCurrentMultiAgentIndex().incrementAndGet();

        log.debug("agent auto assembly loop agent node end");
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return SpringUtil.getBean(MultiAgentNode.class);
    }
}
