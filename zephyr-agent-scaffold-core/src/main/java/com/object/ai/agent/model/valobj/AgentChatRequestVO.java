package com.object.ai.agent.model.valobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent对话请求类")
public class AgentChatRequestVO {

    @Schema(description = "智能体ID")
    private String agentId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "用户消息")
    private String userMessage;

    @Schema(description = "多模态文件 id 列表，对应已上传文件的 id")
    private List<String> fileIds;

}
