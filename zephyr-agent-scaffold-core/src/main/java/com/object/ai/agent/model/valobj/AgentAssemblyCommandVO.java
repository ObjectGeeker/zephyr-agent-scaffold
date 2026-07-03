package com.object.ai.agent.model.valobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent自动装配命令
 *
 * @author object
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent自动装配命令")
public class AgentAssemblyCommandVO {

    @Schema(description = "Agent配置表")
    private AgentConfigTableVO config;

}
