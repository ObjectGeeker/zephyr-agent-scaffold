package com.object.ai.agent.model.valobj;

import com.alibaba.cloud.ai.graph.agent.Agent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent自动装配结果
 *
 * @author object
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent自动装配结果")
public class AgentAssemblyRegisterVO {

    private String appName;

    private String agentName;

    private String agentId;

    private String description;

    private Agent runnerAgent;

}
