package com.object.ai.agent.service.assembly.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.core.collection.CollUtil;
import com.google.adk.agents.LlmAgent;
import com.google.adk.skills.ClassPathSkillSource;
import com.google.adk.skills.InMemorySkillSource;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.skills.SkillSource;
import com.google.adk.tools.skills.SkillToolset;
import com.google.genai.types.Part;
import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.matter.mcp.client.factory.DefaultMcpClientFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentAssemblyAgentNode extends AbstractAgentAssemblySupport {

    @Resource
    private AgentAssemblyMultiAgentNode agentAssemblyMultiAgentNode;

    @Resource
    private DefaultMcpClientFactory defaultMcpClientFactory;

    @Override
    protected AiAgentRegisterVO doApply(AgentAssemblyCommandEntity requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        //1. 获取参数
        List<AiAgentConfigTableVO.Module.Agent> agents = requestParameter.getConfigTable().getModule().getAgents();
        for (AiAgentConfigTableVO.Module.Agent agent : agents) {
            //2. 构造mcp
            List<AiAgentConfigTableVO.Module.Agent.ToolMcp> toolMcpList = agent.getToolMcpList();
            List<Object> allTools = new ArrayList<>();
            if (CollUtil.isNotEmpty(toolMcpList)) {
                for (AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp : toolMcpList) {
                    List<?> tools = defaultMcpClientFactory.getToolMcpCreateService(toolMcp).buildToolCallBack(toolMcp);
                    allTools.addAll(tools);
                }
            }
            //3. 构造SkillToolSet
            if (CollUtil.isNotEmpty(agent.getToolSkillsList())) {
                for (AiAgentConfigTableVO.Module.Agent.ToolSkills toolSkills : agent.getToolSkillsList()) {
                    SkillSource skillSource = null;
                    if ("directory".equals(toolSkills.getType())) {
                        skillSource = new LocalSkillSource(Path.of(toolSkills.getPath()));
                    } else if ("resource".equals(toolSkills.getType())) {
                        skillSource = new ClassPathSkillSource(toolSkills.getPath());
                    }
                    SkillToolset skillToolset = new SkillToolset(skillSource);
                    allTools.add(skillToolset);
                }
            }
            //4. 构造LlmAgent
            LlmAgent llmAgent = LlmAgent.builder()
                    .name(agent.getName())
                    .model(dynamicContext.getLangChain4jModel())
                    .instruction(agent.getInstruction())
                    .description(agent.getDescription())
                    .outputKey(agent.getOutputKey())
                    .tools(allTools)
                    .build();
            //5. 存入上下文
            dynamicContext.getAgentMap().putIfAbsent(llmAgent.name(), llmAgent);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext, AiAgentRegisterVO> get(AgentAssemblyCommandEntity agentAssemblyCommandEntity, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return agentAssemblyMultiAgentNode;
    }
}
