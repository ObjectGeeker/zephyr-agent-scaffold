package com.object.ai.agent.service.assembly.matter.skill.client;

import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;

public interface ToolSkillsCreateService {

    /**
     * 构造skills
     *
     * @param toolSkills skills列表
     * @return tools
     */
    SkillsAgentHook buildToolCallbacks(AgentConfigTableVO.Module.AgentNode.ToolSkills toolSkills);

}
