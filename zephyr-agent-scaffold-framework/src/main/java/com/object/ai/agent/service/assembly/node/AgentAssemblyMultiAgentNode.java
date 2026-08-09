package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.core.collection.CollUtil;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.node.multiagent.AgentAssemblyLoopAgentNode;
import com.object.ai.agent.service.assembly.node.multiagent.AgentAssemblyParallelAgentNode;
import com.object.ai.agent.service.assembly.node.multiagent.AgentAssemblySequentialAgentNode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class AgentAssemblyMultiAgentNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentAssemblyLoopAgentNode agentAssemblyLoopAgentNode;

    @Resource
    private AgentAssemblySequentialAgentNode agentAssemblySequentialAgentNode;

    @Resource
    private AgentAssemblyParallelAgentNode agentAssemblyParallelAgentNode;

    @Resource
    private AgentAssemblyRunnerNode agentAssemblyRunnerNode;

    @Override
    protected AiAgentRegisterVO doApply(AgentAssemblyCommandEntity requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        //1. 获取多智能体配置列表
        List<AiAgentConfigTableVO.Module.MultiAgent> multiAgents = requestParameter.getConfigTable().getModule().getMultiAgents();
        if (null == multiAgents || CollUtil.isEmpty(multiAgents)) {
            return router(requestParameter, dynamicContext);
        }
        //2. 设置动态上下文
        dynamicContext.setMultiAgents(multiAgents);
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> get(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        //1. 判断循环终止条件
        List<AiAgentConfigTableVO.Module.MultiAgent> multiAgents = dynamicContext.getMultiAgents();
        if (null == multiAgents || CollUtil.isEmpty(multiAgents)) {
            return agentAssemblyRunnerNode;
        }
        if (dynamicContext.getCurrentIndex().get() >= multiAgents.size()) {
            return agentAssemblyRunnerNode;
        }
        AiAgentConfigTableVO.Module.MultiAgent multiAgent = multiAgents.get(dynamicContext.getCurrentIndex().get());
        //2. 循环次数++
        dynamicContext.getCurrentIndex().incrementAndGet();
        dynamicContext.setCurrentAgent(multiAgent);

        //3. 根据type判断循环到哪个节点
        return switch (multiAgent.getType()) {
            case "loop" -> agentAssemblyLoopAgentNode;
            case "sequential" -> agentAssemblySequentialAgentNode;
            case "parallel" -> agentAssemblyParallelAgentNode;
            default -> agentAssemblyRunnerNode;
        };
    }
}
