package com.object.ai.file.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.object.ai.common.vo.BaseVO;
import com.object.ai.file.model.enums.FileStatusEnum;
import com.object.ai.file.model.po.FilePO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统文件元数据
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "系统文件")
public class FileVO extends BaseVO {
    @Schema(description = "存储类型，与 file.storage.storage-type 及 ObjectStorageClient#storageType 一致")
    private String storageType;
    @Schema(description = "存储桶名称")
    private String bucket;
    @Schema(description = "对象存储路径（持久标识）")
    private String objectKey;
    @Schema(description = "存储文件名")
    private String fileName;
    @Schema(description = "用户原始文件名")
    private String originName;
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
    @Schema(description = "MIME 类型")
    private String contentType;
    @Schema(description = "文件哈希（SHA-256）")
    private String fileHash;
    @Schema(description = "所属用户 ID")
    private String userId;
    @Schema(description = "业务类型，如 avatar、knowledge")
    private String bizType;
    @Schema(description = "业务关联 ID")
    private String bizId;
    @Schema(description = "状态：UPLOADING-上传中 SUCCESS-成功 FAILED-失败")
    private FileStatusEnum status;
    @Schema(description = "是否临时文件")
    private Boolean isTemp;

    public FilePO toFilePO() {
        FilePO filePO = BeanUtil.copyProperties(this, FilePO.class, "status");
        filePO.setStatus(this.status.name());
        return filePO;
    }
}
