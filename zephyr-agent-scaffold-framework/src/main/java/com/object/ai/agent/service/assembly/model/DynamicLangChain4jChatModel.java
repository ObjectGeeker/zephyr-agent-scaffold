package com.object.ai.agent.service.assembly.model;

import com.object.ai.agent.model.context.ByokThreadContext;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.common.cache.LocalCacheManager;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.Objects;
import java.util.List;
import java.util.Set;

/**
 * 按请求动态解析 LangChain4j OpenAI-compatible ChatModel。
 *
 * <p>ADK 在执行模型回调后才调用该对象，因此 BYOK 插件写入的
 * {@link ByokThreadContext} 可以在本次调用中覆盖默认模型配置。</p>
 */
public class DynamicLangChain4jChatModel implements ChatModel, StreamingChatModel {

    private static final LocalCacheManager<ModelKey, ResolvedModels> MODEL_CACHE =
            new LocalCacheManager<>();

    private final AiAgentConfigTableVO.Module.ChatModel defaultConfig;

    public DynamicLangChain4jChatModel(AiAgentConfigTableVO table) {
        Objects.requireNonNull(table, "table must not be null");
        Objects.requireNonNull(table.getModule(), "table.module must not be null");
        this.defaultConfig = Objects.requireNonNull(
                table.getModule().getChatModel(),
                "table.module.chatModel must not be null");
    }

    /**
     * 覆盖接口默认实现，避免先用默认参数合并 ChatRequest，导致默认模型覆盖 BYOK 模型。
     */
    @Override
    public ChatResponse chat(ChatRequest request) {
        return resolveModels().chatModel().chat(request);
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        return resolveModels().chatModel().doChat(request);
    }

    /**
     * 覆盖接口默认实现，确保流式调用在真正发起请求时解析当前线程的 BYOK 配置。
     */
    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        resolveModels().streamingChatModel().chat(request, handler);
    }

    @Override
    public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        resolveModels().streamingChatModel().doChat(request, handler);
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return resolveModels().chatModel().defaultRequestParameters();
    }

    /**
     * 两个 LangChain4j 模型接口都声明了该默认方法，显式合并以消除 Java 默认方法冲突。
     */
    @Override
    public Set<Capability> supportedCapabilities() {
        return resolveModels().chatModel().supportedCapabilities();
    }

    @Override
    public ModelProvider provider() {
        return resolveModels().chatModel().provider();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return resolveModels().chatModel().listeners();
    }

    private ResolvedModels resolveModels() {
        ByokThreadContext.ByokConfig context = ByokThreadContext.get();
        String model = firstNonBlank(context == null ? null : context.getModelName(), defaultConfig.getModel());
        String configuredBaseUrl = firstNonBlank(
                context == null ? null : context.getBaseUrl(), defaultConfig.getBaseUrl());
        String baseUrl = normalizeBaseUrl(configuredBaseUrl, defaultConfig.getCompletionsPath());
        String apiKey = firstNonBlank(context == null ? null : context.getApiKey(), defaultConfig.getApiKey());

        ModelKey cacheKey = new ModelKey(apiKey, model, baseUrl);
        return MODEL_CACHE.get(cacheKey, () -> new ResolvedModels(
                createChatModel(model, baseUrl, apiKey),
                createStreamingChatModel(model, baseUrl, apiKey)));
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    /**
     * LangChain4j 的 OpenAI 模型接收 API 根地址，而不是完整的
     * /chat/completions 地址。兼容旧配置中 baseUrl 只填写域名的写法。
     */
    static String normalizeBaseUrl(String baseUrl, String completionsPath) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("chatModel.baseUrl must not be blank");
        }

        String normalizedBase = trimTrailingSlashes(baseUrl.trim());
        if (completionsPath == null || completionsPath.isBlank()) {
            return normalizedBase;
        }

        String apiRoot = completionsPath.trim();
        int endpointIndex = apiRoot.indexOf("/chat/completions");
        if (endpointIndex >= 0) {
            apiRoot = apiRoot.substring(0, endpointIndex);
        }
        apiRoot = trimSlashes(apiRoot);
        if (apiRoot.isEmpty()) {
            return normalizedBase;
        }

        String basePath = extractPath(normalizedBase);
        if (basePath.endsWith("/" + apiRoot) || basePath.equals(apiRoot)) {
            return normalizedBase;
        }

        // 例如 baseUrl=https://dashscope.aliyuncs.com，旧 completionsPath=
        // /compatible-mode/v1/chat/completions。
        return normalizedBase + "/" + apiRoot;
    }

    private static String extractPath(String url) {
        int schemeSeparator = url.indexOf("://");
        int pathStart = schemeSeparator >= 0 ? url.indexOf('/', schemeSeparator + 3) : url.indexOf('/');
        return pathStart < 0 ? "" : trimTrailingSlashes(url.substring(pathStart));
    }

    private static String trimSlashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(start, end);
    }

    private static String trimTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static OpenAiChatModel createChatModel(String model, String baseUrl, String apiKey) {
        return OpenAiChatModel.builder()
                .modelName(model)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    private static OpenAiStreamingChatModel createStreamingChatModel(String model, String baseUrl, String apiKey) {
        return OpenAiStreamingChatModel.builder()
                .modelName(model)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    private record ResolvedModels(OpenAiChatModel chatModel, OpenAiStreamingChatModel streamingChatModel) {
    }

    private record ModelKey(String apiKey, String model, String baseUrl) {
    }
}
