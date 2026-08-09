package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.models.langchain4j.LangChain4j;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.model.DynamicLangChain4jChatModel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AgentAssemblyChatModelNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentAssemblyAgentNode  agentAssemblyAgentNode;

    @Override
    protected AiAgentRegisterVO doApply(AgentAssemblyCommandEntity requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        //1. 获取参数
        AiAgentConfigTableVO configTable = requestParameter.getConfigTable();
        //2. 构造 LangChain4j ChatModel，并通过 ADK 适配器接入 Agent。
        DynamicLangChain4jChatModel dynamicModel = new DynamicLangChain4jChatModel(configTable);
        String defaultModelName = configTable.getModule().getChatModel().getModel();
        LangChain4j adkModel = LangChain4j.builder()
                .chatModel(dynamicModel)
                .streamingChatModel(dynamicModel)
                .modelName(defaultModelName)
                .build();
        dynamicContext.setLangChain4jModel(adkModel);
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> get(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return agentAssemblyAgentNode;
    }
}
