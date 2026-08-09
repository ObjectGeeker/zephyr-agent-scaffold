package com.object.ai.agent.service.assembly.factory;

import com.google.adk.agents.BaseAgent;
import com.google.adk.models.langchain4j.LangChain4j;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.assembly.node.AgentAssemblyRootNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认Agent自动装配工厂
 */
@Component
public class DefaultAgentAssemblyFactory {

    @Resource
    private AgentAssemblyRootNode rootNode;

    public AgentAssemblyRootNode rootNode() {
        return rootNode;
    }

    /**
     * 动态上下文对象
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DynamicContext extends cn.bugstack.wrench.design.framework.tree.DynamicContext {

        private LangChain4j langChain4jModel;

        private Map<String, BaseAgent> agentMap = new HashMap<>();

        private List<AiAgentConfigTableVO.Module.MultiAgent> multiAgents;

        private AtomicInteger currentIndex = new AtomicInteger(0);

        private AiAgentConfigTableVO.Module.MultiAgent currentAgent;

    }

}
