package com.object.ai.agent.model.valobj;

import com.google.adk.runner.Runner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent装配完成后注册的对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentRegisterVO {

    private String agentId;

    private String appName;

    private String agentName;

    private String agentDesc;

    private Runner runner;

}
