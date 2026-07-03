package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.object.ai.agent.model.enums.AgentTypeEnum;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * ChatModel节点
 *
 * @author object
 */
@Component
public class AgentChatModelNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentSubAgentNode agentSubAgentNode;

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly chat model node start");

        String appType = requestParameter.getConfig().getAppType();
        if (AgentTypeEnum.openai.name().equals(appType)) {
            ChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(dynamicContext.getOpenAiApi())
                    .defaultOptions(
                            OpenAiChatOptions.builder()
                                    .model(requestParameter.getConfig().getModule().getChatModel().getModel())
                                    .internalToolExecutionEnabled(false)
                                    .build()
                    ).build();

            dynamicContext.setChatModel(chatModel);
        } else if (AgentTypeEnum.dashscope.name().equals(appType)) {
            // dashscope chat model
            ChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dynamicContext.getDashScopeApi())
                    .defaultOptions(
                            DashScopeChatOptions.builder()
                                    .model(requestParameter.getConfig().getModule().getChatModel().getModel())
                                    .internalToolExecutionEnabled(false)
                                    .build()
                    ).build();

            dynamicContext.setChatModel(chatModel);
        }

        log.debug("agent auto assembly chat model node end");
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return agentSubAgentNode;
    }
}
