package com.object.ai.agent.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建 Agent 会话响应体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentSessionCreateResponse {

    private String agentId;

    private String userId;

    private String sessionId;
}
