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
@Schema(description = "更新会话请求")
public class SessionUpdateDTO {

    @Schema(description = "会话 ID")
    private String id;

    @Schema(description = "会话名称")
    private String sessionName;

    @Schema(description = "智能体 ID")
    private String agentId;

    @Schema(description = "会话状态：active / archived")
    private String status;
}
