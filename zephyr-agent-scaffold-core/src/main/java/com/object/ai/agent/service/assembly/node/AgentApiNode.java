package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.object.ai.agent.model.enums.AgentTypeEnum;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.apiplugin.ContextualDashScopeApi;
import com.object.ai.agent.service.assembly.apiplugin.ContextualOpenAiApi;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import jakarta.annotation.Resource;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * Api 节点装配
 */
@Component
public class AgentApiNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentChatModelNode agentChatModelNode;

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly api node start");
        String appType = requestParameter.getConfig().getAppType();
        AgentConfigTableVO.Module.AgentApi api = requestParameter.getConfig().getModule().getApi();
        if (AgentTypeEnum.openai.name().equals(appType)) {
            // OpenAI
            OpenAiApi openAiApi = ContextualOpenAiApi.contextBuilder()
                    .apiKey(api.getApiKey())
                    .baseUrl(api.getBaseUrl())
                    .completionsPath(api.getCompletionsPath())
                    .embeddingsPath(api.getEmbeddingsPath())
                    .build();
            dynamicContext.setOpenAiApi(openAiApi);
        } else if (AgentTypeEnum.dashscope.name().equals(appType)) {
            // dashscope
            DashScopeApi dashScopeApi = ContextualDashScopeApi.contextBuilder().apiKey(api.getApiKey()).build();
            dynamicContext.setDashScopeApi(dashScopeApi);
        }
        log.debug("agent auto assembly api node end");
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return agentChatModelNode;
    }
}
