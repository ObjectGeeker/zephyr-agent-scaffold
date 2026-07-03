package com.object.ai.agent.service.assembly.node.multagent;

import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;

import java.util.List;

public interface CustomizedAgentArmory {

    FlowAgent buildCustomizedAgent(AgentConfigTableVO.Module.MultiAgent currentMultiAgent, List<Agent> subAgents);

}
