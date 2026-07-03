package com.object.ai.agent.service.assembly.matter.skill.client;

import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import org.springframework.stereotype.Service;

@Service
public class DefaultToolSkillsCreateService implements ToolSkillsCreateService {
    @Override
    public SkillsAgentHook buildToolCallbacks(AgentConfigTableVO.Module.AgentNode.ToolSkills toolSkills) {
        String type = toolSkills.getType();
        String path = toolSkills.getPath();

        SkillRegistry registry = null;

        if ("directory".equals(type)) {
            registry = FileSystemSkillRegistry.builder()
                    .projectSkillsDirectory(path)
                    .build();
        }

        if ("resource".equals(type)) {
            registry = ClasspathSkillRegistry.builder()
                    .classpathPath(path)
                    .build();
        }

        return SkillsAgentHook.builder()
                .skillRegistry(registry)
                .build();
    }
}
