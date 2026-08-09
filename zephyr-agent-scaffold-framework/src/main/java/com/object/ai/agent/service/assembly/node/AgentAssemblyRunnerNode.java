package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.google.adk.agents.BaseAgent;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.runner.InMemoryRunner;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.matter.plugin.ByokThreadLocalPlugin;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;

@Component
public class AgentAssemblyRunnerNode extends AbstractAgentAssemblySupport {
    @Resource
    private ByokThreadLocalPlugin byokThreadLocalPlugin;

    @Override
    protected AiAgentRegisterVO doApply(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        // 入参对象
        AiAgentConfigTableVO aiAgentConfigTableVO = agentAssemblyCommandEntity.getConfigTable();
        String appName = aiAgentConfigTableVO.getAppName();
        String agentId = aiAgentConfigTableVO.getAgent().getAgentId();
        String agentName = aiAgentConfigTableVO.getAgent().getAgentName();
        String agentDesc = aiAgentConfigTableVO.getAgent().getAgentDesc();

        // Runner 运行体
        InMemoryRunner memoryRunner = this.createRunner(agentAssemblyCommandEntity, dynamicContext, appName);

        // 构建注册对象
        AiAgentRegisterVO aiAgentRegisterVO = AiAgentRegisterVO.builder()
                .agentId(agentId)
                .appName(appName)
                .agentName(agentName)
                .agentDesc(agentDesc)
                .runner(memoryRunner)
                .build();

        // 注册到Spring容器
        SpringUtil.registerBean("AGENT_" + agentId, aiAgentRegisterVO);

        return aiAgentRegisterVO;
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> get(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

    private InMemoryRunner createRunner(AgentAssemblyCommandEntity requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext, String appName) {
        AiAgentConfigTableVO.Runner runnerConfig = requestParameter.getConfigTable().getRunner();

        // 获取智能体（用这个智能体装配 InMemoryRunner）
        BaseAgent baseAgent = dynamicContext.getAgentMap().get(runnerConfig.getAgentName());

        // 装配plugin
        ArrayList<BasePlugin> plugins = CollUtil.newArrayList();
        // BYOK 参数来自每次请求的 RunConfig.customMetadata，必须默认启用，
        // 否则请求中的 model/apiKey/baseUrl 不会进入动态模型上下文。
        plugins.add(byokThreadLocalPlugin);
        if (CollUtil.isNotEmpty(runnerConfig.getPluginNameList())) {
            for (String pluginName : runnerConfig.getPluginNameList()) {
                BasePlugin pluginBean = SpringUtil.getBean(pluginName, BasePlugin.class);
                if (!plugins.contains(pluginBean)) {
                    plugins.add(pluginBean);
                }
            }
        }
        // 会话运行节点
        return new InMemoryRunner(baseAgent, appName, plugins);
    }
}
