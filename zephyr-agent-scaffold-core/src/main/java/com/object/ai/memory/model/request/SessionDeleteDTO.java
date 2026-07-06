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
@Schema(description = "删除会话请求")
public class SessionDeleteDTO {

    @Schema(description = "会话 ID")
    private String id;
}
