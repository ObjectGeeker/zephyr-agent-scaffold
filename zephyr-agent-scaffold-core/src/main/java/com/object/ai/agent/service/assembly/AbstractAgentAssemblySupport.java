package com.object.ai.agent.service.assembly;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class AbstractAgentAssemblySupport extends AbstractMultiThreadStrategyRouter<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractAgentAssemblySupport.class);

    @Override
    protected void multiThread(AgentAssemblyCommandVO agentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

    protected void buildMultiAgent(DefaultAgentAssemblyFactory.DynamicContext dynamicContext, BiFunction<AgentConfigTableVO.Module.MultiAgent, List<Agent>, Agent> buildAgent) {
        AgentConfigTableVO.Module.MultiAgent currentMultiAgent = dynamicContext.getCurrentMultiAgent();
        List<String> subAgentKeys = currentMultiAgent.getSubAgentKeys();
        List<Agent> subAgents = new ArrayList<>();
        if (CollUtil.isNotEmpty(subAgentKeys)) {
            subAgents.addAll(subAgentKeys.stream().map(key -> dynamicContext.getAgentMap().get(key)).collect(Collectors.toList()));
        }

        Agent agent = buildAgent.apply(currentMultiAgent, subAgents);

        SpringUtil.registerBean(currentMultiAgent.getKey(), agent);

        dynamicContext.getAgentMap().put(currentMultiAgent.getKey(), agent);
    }
}
