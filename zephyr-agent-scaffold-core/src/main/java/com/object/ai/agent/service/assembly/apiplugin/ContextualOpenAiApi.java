package com.object.ai.agent.service.assembly.apiplugin;

import cn.hutool.core.util.StrUtil;
import com.object.ai.agent.model.context.ModelContextHolder;
import com.object.ai.agent.model.context.ModelCredentials;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiStreamFunctionCallingHelper;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ContextualOpenAiApi extends OpenAiApi {

    private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    private static final String REQUEST_BODY_NULL_MESSAGE = "The request body can not be null.";

    private static final String STREAM_FALSE_MESSAGE = "Request must set the stream property to false.";

    private static final String ADDITIONAL_HEADERS_NULL_MESSAGE = "The additional HTTP headers can not be null.";

    private final WebClient webClient;

    private final MultiValueMap<String, String> headers;

    private final Consumer<HttpHeaders> finalHeaders;

    private final String completionsPath;

    private final ApiKey apiKey;

    private final WebClient.Builder webClientBuilder;

    private final ResponseErrorHandler responseErrorHandler;

    private OpenAiStreamFunctionCallingHelper chunkMerger = new OpenAiStreamFunctionCallingHelper();

    public ContextualOpenAiApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers, String completionsPath, String embeddingsPath, RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
        super(baseUrl, apiKey, headers, completionsPath, embeddingsPath, restClientBuilder, webClientBuilder, responseErrorHandler);
        // @formatter:off
        Consumer<HttpHeaders> finalHeaders = h -> {
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set(HTTP_USER_AGENT_HEADER, SPRING_AI_USER_AGENT);
            h.addAll(headers);
        };
        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(finalHeaders)
                .build(); // @formatter:on
        this.headers = headers;
        this.finalHeaders = finalHeaders;
        this.completionsPath = completionsPath;
        this.apiKey = apiKey;
        this.webClientBuilder = webClientBuilder;
        this.responseErrorHandler = responseErrorHandler;
    }

    public static ContextualOpenAiApi.Builder contextBuilder() {
        return new ContextualOpenAiApi.Builder();
    }

    public static final class Builder {
        private String baseUrl = "https://api.openai.com";
        private ApiKey apiKey;
        private MultiValueMap<String, String> headers = new LinkedMultiValueMap();
        private String completionsPath = "/v1/chat/completions";
        private String embeddingsPath = "/v1/embeddings";
        private RestClient.Builder restClientBuilder = RestClient.builder();
        private WebClient.Builder webClientBuilder = WebClient.builder();
        private ResponseErrorHandler responseErrorHandler;

        public Builder() {
            this.responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;
        }

        public ContextualOpenAiApi.Builder baseUrl(String baseUrl) {
            Assert.hasText(baseUrl, "baseUrl cannot be null or empty");
            this.baseUrl = baseUrl;
            return this;
        }

        public ContextualOpenAiApi.Builder apiKey(ApiKey apiKey) {
            Assert.notNull(apiKey, "apiKey cannot be null");
            this.apiKey = apiKey;
            return this;
        }

        public ContextualOpenAiApi.Builder apiKey(String simpleApiKey) {
            this.apiKey = new SimpleApiKey(simpleApiKey);
            return this;
        }

        public ContextualOpenAiApi.Builder headers(MultiValueMap<String, String> headers) {
            Assert.notNull(headers, "headers cannot be null");
            this.headers = headers;
            return this;
        }

        public ContextualOpenAiApi.Builder completionsPath(String completionsPath) {
            Assert.hasText(completionsPath, "completionsPath cannot be null or empty");
            this.completionsPath = completionsPath;
            return this;
        }

        public ContextualOpenAiApi.Builder embeddingsPath(String embeddingsPath) {
            Assert.hasText(embeddingsPath, "embeddingsPath cannot be null or empty");
            this.embeddingsPath = embeddingsPath;
            return this;
        }

        public ContextualOpenAiApi.Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            Assert.notNull(restClientBuilder, "restClientBuilder cannot be null");
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        public ContextualOpenAiApi.Builder webClientBuilder(WebClient.Builder webClientBuilder) {
            Assert.notNull(webClientBuilder, "webClientBuilder cannot be null");
            this.webClientBuilder = webClientBuilder;
            return this;
        }

        public ContextualOpenAiApi.Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
            Assert.notNull(responseErrorHandler, "responseErrorHandler cannot be null");
            this.responseErrorHandler = responseErrorHandler;
            return this;
        }

        public ContextualOpenAiApi build() {
            Assert.notNull(this.apiKey, "apiKey must be set");
            return new ContextualOpenAiApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath, this.embeddingsPath, this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
        }
    }

    @Override
    public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest, MultiValueMap<String,String> additionalHttpHeader) {
        // ✨核心替换逻辑
        ModelCredentials modelCredentials = ModelContextHolder.get();
        if (null != modelCredentials) {
            String apiKey = modelCredentials.getApiKey();
            String model = modelCredentials.getModel();
            if (StrUtil.isNotBlank(apiKey)) {
                additionalHttpHeader.add(HttpHeaders.AUTHORIZATION, apiKey);
            }
            if (StrUtil.isNotBlank(model)) {
                chatRequest = ModelOptionsUtils.merge(OpenAiChatOptions.builder()
                        .model(model)
                        .build(), chatRequest, ChatCompletionRequest.class);
            }
        }
        Assert.notNull(chatRequest, REQUEST_BODY_NULL_MESSAGE);
        Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

        AtomicBoolean isInsideTool = new AtomicBoolean(false);

        // ✨直接替换请求的数据
        WebClient threadWebClient = this.webClient;
        if (StrUtil.isNotBlank(modelCredentials.getBaseUrl())) {
            threadWebClient = webClientBuilder.baseUrl(modelCredentials.getBaseUrl()).defaultHeaders(finalHeaders).build();
        }

        // @formatter:off
        return threadWebClient.post()
                .uri(StrUtil.isNotBlank(modelCredentials.getCompletionsPath()) ? modelCredentials.getCompletionsPath() : this.completionsPath)
                .headers(headers -> {
                    headers.addAll(additionalHttpHeader);
                    addDefaultHeadersIfMissing(headers);
                }) // @formatter:on
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToFlux(String.class)
                // cancels the flux stream after the "[DONE]" is received.
                .takeUntil(SSE_DONE_PREDICATE)
                // filters out the "[DONE]" message.
                .filter(SSE_DONE_PREDICATE.negate())
                .map(content -> ModelOptionsUtils.jsonToObject(content, ChatCompletionChunk.class))
                // Detect is the chunk is part of a streaming function call.
                .map(chunk -> {
                    if (this.chunkMerger.isStreamingToolFunctionCall(chunk)) {
                        isInsideTool.set(true);
                    }
                    return chunk;
                })
                // Group all chunks belonging to the same function call.
                // Flux<ChatCompletionChunk> -> Flux<Flux<ChatCompletionChunk>>
                .windowUntil(chunk -> {
                    if (isInsideTool.get() && this.chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
                        isInsideTool.set(false);
                        return true;
                    }
                    return !isInsideTool.get();
                })
                // Merging the window chunks into a single chunk.
                // Reduce the inner Flux<ChatCompletionChunk> window into a single
                // Mono<ChatCompletionChunk>,
                // Flux<Flux<ChatCompletionChunk>> -> Flux<ChatCompletionChunk>>
                // @formatter:off
                .concatMap(window -> window.reduce(
                        new ChatCompletionChunk(null, null, null, null, null, null, null, null),
                        (previous, current) -> this.chunkMerger.merge(previous, current)));
    }

    private void addDefaultHeadersIfMissing(HttpHeaders headers) {
        if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && !(this.apiKey instanceof NoopApiKey)) {
            headers.setBearerAuth(this.apiKey.getValue());
        }
    }
}
