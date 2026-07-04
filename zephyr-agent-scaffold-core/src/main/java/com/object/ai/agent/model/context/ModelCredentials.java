package com.object.ai.agent.model.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelCredentials {
    private String apiKey;
    private String model;
    private String baseUrl;
    private String completionsPath;
    private String embeddingPath;

    /**
     * 标记是否多模态对话
     */
    private Boolean multiModel;
}