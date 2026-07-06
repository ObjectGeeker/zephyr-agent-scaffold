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
@Schema(description = "消息列表查询请求")
public class MessageQueryDTO {

    @Schema(description = "会话 ID")
    private String sessionId;
}
