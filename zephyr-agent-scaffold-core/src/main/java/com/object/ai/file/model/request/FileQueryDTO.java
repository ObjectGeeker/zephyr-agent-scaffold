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
@Schema(description = "文件查询请求")
public class FileQueryDTO {

    @Schema(description = "存储类型")
    private String storageType;
    @Schema(description = "存储桶名称")
    private String bucket;
    @Schema(description = "对象存储路径")
    private String objectKey;
    @Schema(description = "用户原始文件名")
    private String originName;
    @Schema(description = "所属用户 ID")
    private String userId;
    @Schema(description = "业务类型")
    private String bizType;
    @Schema(description = "业务关联 ID")
    private String bizId;
    @Schema(description = "文件状态")
    private String status;
    @Schema(description = "是否临时文件")
    private Boolean isTemp;
}
