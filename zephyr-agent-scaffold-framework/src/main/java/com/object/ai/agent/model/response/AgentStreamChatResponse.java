package com.object.ai.agent.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent流式对话返回体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentStreamChatResponse {

    private String agentName;

    private String content;

    private String toolCallName;

    private String toolCallResponse;

}
