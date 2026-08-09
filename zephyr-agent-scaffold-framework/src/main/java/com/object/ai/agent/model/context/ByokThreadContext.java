package com.object.ai.agent.model.context;

import lombok.Data;

/**
 * BYOK（Bring Your Own Key）线程上下文：用单一 ThreadLocal 持有当前线程的完整模型配置。
 * 使用 InheritableThreadLocal 以便子线程（如异步响应式链路）继承上下文。
 */
public class ByokThreadContext {

    private static final ThreadLocal<ByokConfig> CONTEXT = new InheritableThreadLocal<>();

    public static void set(ByokConfig config) {
        CONTEXT.set(config);
    }

    public static ByokConfig get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * BYOK 模型配置。各字段允许为 null/空白，缺失字段由消费方回退到 Agent 装配表的默认配置。
     */
    @Data
    public static class ByokConfig {

        private String modelName;

        private String baseUrl;

        private String apiKey;

        public ByokConfig() {
        }

        public ByokConfig(String modelName, String baseUrl, String apiKey) {
            this.modelName = modelName;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
        }
    }
}
