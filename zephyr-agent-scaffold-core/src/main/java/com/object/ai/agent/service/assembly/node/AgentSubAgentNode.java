package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.object.ai.agent.model.enums.AgentSaverTypeEnum;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import com.object.ai.agent.service.assembly.matter.mcp.client.factory.DefaultMcpCreateFactory;
import com.object.ai.agent.service.assembly.matter.skill.client.DefaultToolSkillsCreateService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 子Agent装配
 *
 * @author object
 */
@Component
public class AgentSubAgentNode extends AbstractAgentAssemblySupport {

    @Resource
    private DefaultMcpCreateFactory defaultMcpCreateFactory;

    @Resource
    private DefaultToolSkillsCreateService defaultToolSkillsCreateService;

    @Resource
    private MultiAgentNode multiAgentNode;

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly sub agent node start");

        List<AgentConfigTableVO.Module.AgentNode> agentNodes = requestParameter.getConfig().getModule().getAgentNodes();
        log.debug("agent auto assembly sub agent size {}", agentNodes.size());

        for (AgentConfigTableVO.Module.AgentNode agentNodeConfig : agentNodes) {
            List<ToolCallback> toolCallbackList = new ArrayList<>();
            List<SkillsAgentHook> skillsAgentHooks = new ArrayList<>();
            // 构造mcp
            if (CollUtil.isNotEmpty(agentNodeConfig.getToolMcpList())) {
                for (AgentConfigTableVO.Module.AgentNode.ToolMcp toolMcp : agentNodeConfig.getToolMcpList()) {
                    ToolMcpCreateService toolMcpCreateService = defaultMcpCreateFactory.getToolMcpCreateService(toolMcp);
                    ToolCallback[] toolCallbacks = toolMcpCreateService.buildToolCallbacks(toolMcp);
                    toolCallbackList.addAll(List.of(toolCallbacks));
                }
            }
            // 构建skill服务
            if (CollUtil.isNotEmpty(agentNodeConfig.getToolSkills())) {
                for (AgentConfigTableVO.Module.AgentNode.ToolSkills toolSkills : agentNodeConfig.getToolSkills()) {
                    SkillsAgentHook hook = defaultToolSkillsCreateService.buildToolCallbacks(toolSkills);
                    skillsAgentHooks.add(hook);
                }
            }

            // 构造hooks
            List<Hook> hookList = new ArrayList<>(skillsAgentHooks);
            if (CollUtil.isNotEmpty(agentNodeConfig.getHooks())) {
                List<String> hooks = agentNodeConfig.getHooks();
                for (String hookName : hooks) {
                    Hook hookBean = SpringUtil.getBean(hookName, Hook.class);
                    hookList.add(hookBean);
                }
            }

            // 构造interceptors
            List<Interceptor> interceptorList = new ArrayList<>();
            if (CollUtil.isNotEmpty(agentNodeConfig.getInterceptors())) {
                List<String> interceptors = agentNodeConfig.getInterceptors();
                for (String interceptorName : interceptors) {
                    Interceptor interceptorBean = SpringUtil.getBean(interceptorName, Interceptor.class);
                    interceptorList.add(interceptorBean);
                }
            }

            // 构造saver 默认memory
            BaseCheckpointSaver saver = new MemorySaver();
            if (AgentSaverTypeEnum.memory.name().equals(agentNodeConfig.getSaverType())) {
                saver = new MemorySaver();
            } else if (AgentSaverTypeEnum.redis.name().equals(agentNodeConfig.getSaverType())) {
                // 需引入redissonClient
                // saver = new RedisSaver();
            } else if (AgentSaverTypeEnum.mongodb.name().equals(agentNodeConfig.getSaverType())) {
                // 需引入mongodb
                //  saver = new MongoSaver();
            } else if (AgentSaverTypeEnum.custom.name().equals(agentNodeConfig.getSaverType())) {
                String saverClass = agentNodeConfig.getSaverClass();
                saver = SpringUtil.getBean(saverClass, BaseCheckpointSaver.class);
            }

            // 构造agent
            Builder builder = ReactAgent.builder();
            builder.name(agentNodeConfig.getName())
                    .model(dynamicContext.getChatModel())
                    .description(agentNodeConfig.getDescription())
                    .systemPrompt(agentNodeConfig.getSystemPrompt())
                    .instruction(agentNodeConfig.getInstruction())
                    .saver(saver)
                    .outputKey(agentNodeConfig.getOutputKey())
                    .returnReasoningContents(agentNodeConfig.getReturnReasoningContents())
                    .outputSchema(agentNodeConfig.getOutputSchema())
                    .parallelToolExecution(agentNodeConfig.getParallelToolExecution())
                    .maxParallelTools(agentNodeConfig.getMaxParallelTools())
                    .interceptors(interceptorList)
                    .hooks(hookList)
                    .tools(toolCallbackList)
                    .build();
            if (StrUtil.isNotBlank(agentNodeConfig.getOutputType())) {
                builder.outputType(ClassUtil.loadClass(agentNodeConfig.getOutputType()));
            }
            if (null != agentNodeConfig.getToolExecutionTimeout()) {
                builder.toolExecutionTimeout(Duration.ofSeconds(agentNodeConfig.getToolExecutionTimeout()));
            }
            ReactAgent agentNode = builder.build();


            // 注入Spring容器
            SpringUtil.registerBean(agentNodeConfig.getKey(), agentNode);

            // 放入Map
            dynamicContext.getAgentMap().put(agentNodeConfig.getKey(), agentNode);
        }

        log.debug("agent auto assembly sub agent node end");
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return multiAgentNode;
    }
}
