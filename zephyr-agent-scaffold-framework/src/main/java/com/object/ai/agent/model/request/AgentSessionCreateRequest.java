package com.object.ai.agent.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建 Agent 会话请求体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentSessionCreateRequest {

    private String agentId;

    private String userId;
}
