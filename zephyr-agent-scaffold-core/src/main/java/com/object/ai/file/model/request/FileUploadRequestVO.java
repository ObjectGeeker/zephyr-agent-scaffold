package com.object.ai.file.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "文件上传请求")
public class FileUploadRequestVO {

    @Schema(description = "业务类型")
    private String bizType;
    @Schema(description = "业务关联 ID")
    private String bizId;

}