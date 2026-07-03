package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.core.collection.CollUtil;
import com.object.ai.agent.model.enums.MultiAgentTypeEnum;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.node.multagent.CustomizedAgentNode;
import com.object.ai.agent.service.assembly.node.multagent.LoopAgentNode;
import com.object.ai.agent.service.assembly.node.multagent.ParallelAgentNode;
import com.object.ai.agent.service.assembly.node.multagent.RoutingAgentNode;
import com.object.ai.agent.service.assembly.node.multagent.SequentialAgentNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 多Agent装配
 *
 * @author object
 */
@Component
public class MultiAgentNode extends AbstractAgentAssemblySupport {

    @Resource
    private SequentialAgentNode sequentialAgentNode;

    @Resource
    private LoopAgentNode loopAgentNode;

    @Resource
    private ParallelAgentNode parallelAgentNode;

    @Resource
    private RoutingAgentNode routingAgentNode;

    @Resource
    private CustomizedAgentNode customizedAgentNode;

    @Resource
    private AgentRunnerNode agentRunnerNode;

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly multi agent node start");
        List<AgentConfigTableVO.Module.MultiAgent> multiAgents = requestParameter.getConfig().getModule().getMultiAgents();

        if (CollUtil.isEmpty(multiAgents) || dynamicContext.getCurrentMultiAgentIndex().get() >= multiAgents.size()) {
            dynamicContext.setCurrentMultiAgent(null);
            return router(requestParameter, dynamicContext);
        }

        AgentConfigTableVO.Module.MultiAgent currentMultiAgent = multiAgents.get(dynamicContext.getCurrentMultiAgentIndex().get());

        dynamicContext.setCurrentMultiAgent(currentMultiAgent);

        log.debug("agent auto assembly multi agent node end");

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        AgentConfigTableVO.Module.MultiAgent currentMultiAgent = dynamicContext.getCurrentMultiAgent();
        if (currentMultiAgent == null) {
            return agentRunnerNode;
        }

        String type = currentMultiAgent.getType();
        MultiAgentTypeEnum agentTypeEnum = MultiAgentTypeEnum.valueOf(type);

        String node = agentTypeEnum.name();

        return switch (node) {
            case "sequential" -> sequentialAgentNode;
            case "loop" -> loopAgentNode;
            case "parallel" -> parallelAgentNode;
            case "routing" -> routingAgentNode;
            case "customized" -> customizedAgentNode;
            default -> agentRunnerNode;
        };
    }
}
