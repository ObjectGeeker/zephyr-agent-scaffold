package com.object.ai.agent.model.valobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Agent流式对话响应对象
 *
 * @author object
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent流式对话响应对象")
public class AgentStreamResponseVO implements Serializable {

    /**
     * 消息类型：assistant or tool
     */
    private String type;

    /**
     * 本次应答的智能体名称
     */
    private String agentName;

    /**
     * 本次应答的消息内容
     */
    private String content;

    /**
     * 本次调用的工具ID
     */
    private String toolCallId;

    /**
     * 本次调用的工具名称
     */
    private String toolCallName;

    /**
     * 本次调用的工具响应内容
     */
    private String toolCallResponse;

}
