package com.object.ai.memory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "创建会话请求")
public class SessionCreateDTO {

    @Schema(description = "会话 ID，可选；不传则由服务端生成，同时作为 Agent threadId")
    private String id;

    @Schema(description = "会话名称")
    private String sessionName;

    @Schema(description = "智能体 ID")
    private String agentId;
}
