package com.object.ai.agent.model.entity;

import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent自动装配命令实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentAssemblyCommandEntity {

    private AiAgentConfigTableVO configTable;

}
