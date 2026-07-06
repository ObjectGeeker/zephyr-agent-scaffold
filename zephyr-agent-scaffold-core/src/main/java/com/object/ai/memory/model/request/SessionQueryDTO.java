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
@Schema(description = "会话列表查询请求")
public class SessionQueryDTO {

    @Schema(description = "智能体 ID")
    private String agentId;

    @Schema(description = "会话状态：active / archived")
    private String status;

    @Schema(description = "会话名称（模糊匹配）")
    private String sessionName;
}
