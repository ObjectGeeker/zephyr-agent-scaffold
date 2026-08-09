package com.object.ai.agent.service.assembly;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import com.google.adk.agents.BaseAgent;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public abstract class AbstractAgentAssemblySupport extends AbstractMultiThreadStrategyRouter<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> {

    protected final Logger log = LoggerFactory.getLogger(AbstractAgentAssemblySupport.class);

    @Override
    protected void multiThread(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

    protected List<BaseAgent> queryAgentList(DefaultAgentAssemblyFactory.DynamicContext dynamicContext, List<String> agentNames) {
        Map<String, BaseAgent> agentMap = dynamicContext.getAgentMap();
        return agentNames.stream().map(agentMap::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
