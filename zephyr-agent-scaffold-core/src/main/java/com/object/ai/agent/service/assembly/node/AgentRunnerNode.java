package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import org.springframework.stereotype.Component;

/**
 * Agent运行装配
 *
 * @author object
 */
@Component
public class AgentRunnerNode extends AbstractAgentAssemblySupport {
    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly agent runner node start");

        AgentConfigTableVO.Module.AgentRunner agentRunner = requestParameter.getConfig().getModule().getAgentRunner();
        String runAgentKey = agentRunner.getRunAgentKey();
        Agent runner = dynamicContext.getAgentMap().get(runAgentKey);

        AgentAssemblyRegisterVO agentAutoArmoryRegisterVO = AgentAssemblyRegisterVO.builder()
                .runnerAgent(runner)
                .appName(requestParameter.getConfig().getAppName())
                .agentId(requestParameter.getConfig().getAgent().getAgentId())
                .agentName(requestParameter.getConfig().getAgent().getAgentName())
                .description(requestParameter.getConfig().getAgent().getDescription())
                .build();

        SpringUtil.registerBean("Agent_" + requestParameter.getConfig().getAgent().getAgentId(), agentAutoArmoryRegisterVO);

        log.debug("agent auto assembly agent runner node end");
        return agentAutoArmoryRegisterVO;
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
