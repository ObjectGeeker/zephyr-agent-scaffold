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
@Schema(description = "文件保存请求")
public class FileUpdateDTO {

    @Schema(description = "文件记录 ID；新增时可传上传接口返回的临时文件 ID，更新/删除时传旧文件记录 ID")
    private String id;
    @Schema(description = "新上传的临时文件 ID；更新替换文件时使用")
    private String fileId;
    @Schema(description = "用户原始文件名")
    private String originName;
    @Schema(description = "业务类型")
    private String bizType;
}
