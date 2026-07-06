package com.object.ai.memory.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.object.ai.agent.model.enums.AgentTypeEnum;
import com.object.ai.memory.model.properties.MemorySummarizationChatModelProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 长期记忆配置类
 */
@Configuration
@EnableConfigurationProperties(MemorySummarizationChatModelProperties.class)
public class LongTermMemoryConfiguration {

    @Bean("memoryTaskExecutor")
    @ConditionalOnBooleanProperty(prefix = "long-term-memory", value = "enabled")
    public ThreadPoolTaskExecutor memoryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("memory-summary-");
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnBooleanProperty(prefix = "long-term-memory", value = "enabled")
    public ChatModel longTermMemoryChatModel(MemorySummarizationChatModelProperties properties) {
        String type = properties.getType();
        if (AgentTypeEnum.openai.name().equals(type)) {
            return buildWithOpenAi(properties);
        } else if (AgentTypeEnum.dashscope.name().equals(type)) {
            return buildWithDashScope(properties);
        }
        return null;
    }

    private ChatModel buildWithDashScope(MemorySummarizationChatModelProperties properties) {
        DashScopeApi api = DashScopeApi.builder()
                .apiKey(properties.getApiKey())
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(DashScopeChatOptions.builder().model(properties.getModel()).build())
                .build();
    }

    private ChatModel buildWithOpenAi(MemorySummarizationChatModelProperties properties) {
        OpenAiApi api = OpenAiApi
                .builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(properties.getModel()).build())
                .build();
    }
}
