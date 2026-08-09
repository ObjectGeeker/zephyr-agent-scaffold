package com.object.ai.agent.service.assembly.node.multiagent;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.extra.spring.SpringUtil;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ParallelAgent;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.node.AgentAssemblyMultiAgentNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentAssemblyParallelAgentNode extends AbstractAgentAssemblySupport {
    @Override
    protected AiAgentRegisterVO doApply(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        //1. 获取配置
        AiAgentConfigTableVO.Module.MultiAgent currentAgent = dynamicContext.getCurrentAgent();
        //2. 查询子Agent
        List<BaseAgent> subAgents = queryAgentList(dynamicContext, currentAgent.getSubAgents());
        //3. 构造Agent
        ParallelAgent parallelAgent =
                ParallelAgent.builder()
                        .name(currentAgent.getName())
                        .description(currentAgent.getDescription())
                        .subAgents(subAgents)
                        .build();
        //4. 放入Map
        dynamicContext.getAgentMap().putIfAbsent(parallelAgent.name(), parallelAgent);
        return router(agentAssemblyCommandEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> get(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return SpringUtil.getBean(AgentAssemblyMultiAgentNode.class);
    }
}
