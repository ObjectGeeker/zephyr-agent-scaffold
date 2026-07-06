package com.object.ai.memory.model.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 长期记忆属性类
 */
@ConfigurationProperties(prefix = "long-term-memory")
@Data
public class MemorySummarizationChatModelProperties {

    private Boolean enabled = false;

    /**
     * openai or dashscope
     */
    private String type;

    private String baseUrl;

    private String apiKey;

    private String model;

}
