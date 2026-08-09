package com.object.ai.agent.model.response;

import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置的智能体信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentInfoDTO {

    private String agentId;

    private String agentName;

    private String agentDesc;

    public static AiAgentInfoDTO toAiAgentInfo(AiAgentConfigTableVO table) {
        return AiAgentInfoDTO.builder()
                .agentName(table.getAgent().getAgentName())
                .agentId(table.getAgent().getAgentId())
                .agentDesc(table.getAgent().getAgentDesc())
                .build();
    }

}
