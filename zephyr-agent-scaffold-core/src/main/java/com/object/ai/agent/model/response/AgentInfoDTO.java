package com.object.ai.agent.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "查询Agent响应体")
public class AgentInfoDTO {

    private String agentId;

    private String agentName;

    private String description;

}
