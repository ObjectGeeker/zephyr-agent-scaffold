package com.object.ai.agent.model.valobj;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent流式对话请求参数
 *
 * @author object
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent流式对话请求参数")
public class AgentStreamRequestVO implements Serializable {

    @NotBlank(message = "用户消息不能为空")
    @Schema(description = "用户输入消息")
    private String message;

    @Schema(description = "会话ID，用于维持对话上下文")
    private String threadId;

    @Schema(description = "运行的Agent Bean名称，为空时使用默认装配的Runner Agent")
    private String agentName;

    @Schema(description = "agentId")
    private String agentId;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "APIKEY")
    private String apiKey;

    @Schema(description = "请求地址")
    private String baseUrl;

    @Schema(description = "对话接口")
    private String completionPath;

    @Schema(description = "多模态文件 id 列表，对应已上传文件的 id")
    private List<String> fileIds;

    @Schema(description = "多模态接口")
    private String multiModelPath;

    @Schema(description = "是否将本轮对话消息持久化到数据库")
    private Boolean saveMessage;

}
