package com.object.ai.agent.service.assembly.apiplugin;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAiStreamFunctionCallingHelper;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import com.object.ai.agent.model.context.ModelContextHolder;
import com.object.ai.agent.model.context.ModelCredentials;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.ENABLED;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.HEADER_SSE;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.MULTIMODAL_GENERATION_RESTFUL_URL;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.TEXT_EMBEDDING_RESTFUL_URL;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.TEXT_GENERATION_RESTFUL_URL;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.TEXT_RERANK_RESTFUL_URL;

public class ContextualDashScopeApi extends DashScopeApi {

    private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    private final String completionsPath;

    private final WebClient.Builder webClientBuilder;

    private final WebClient webClient;

    private final ApiKey apiKey;

    private final MultiValueMap<String, String> header;

    private final Consumer<HttpHeaders> finalHeaders;

    /**
     * Create a new chat completion api.
     *
     * @param baseUrl              api base URL.
     * @param apiKey               OpenAI apiKey.
     * @param header               the http headers to use.
     * @param workSpaceId          the workspace ID to use.
     * @param completionsPath      the path to the chat completions endpoint.
     * @param embeddingsPath       the path to the embeddings endpoint.
     * @param rerankPath           the path to the rerank endpoint.
     * @param restClientBuilder    RestClient builder.
     * @param webClientBuilder     WebClient builder.
     * @param responseErrorHandler Response error handler.
     */
    public ContextualDashScopeApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> header, String workSpaceId, String completionsPath, String embeddingsPath, String rerankPath, RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
        super(baseUrl, apiKey, header, workSpaceId, completionsPath, embeddingsPath, rerankPath, restClientBuilder, webClientBuilder, responseErrorHandler);
        this.completionsPath = completionsPath;
        this.webClientBuilder = webClientBuilder;
        this.apiKey = apiKey;
        this.header = header;
        // Check API Key in headers.
        Consumer<HttpHeaders> finalHeaders = h -> {
            if (!(apiKey instanceof NoopApiKey)) {
                h.setBearerAuth(apiKey.getValue());
            }

            h.setContentType(MediaType.APPLICATION_JSON);
            h.addAll(header);
        };
        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(finalHeaders)
                .build();
        this.finalHeaders = finalHeaders;
    }

    public static ContextualDashScopeApi.Builder contextBuilder() {
        return new ContextualDashScopeApi.Builder();
    }

    public static class Builder {

        private String baseUrl = DEFAULT_BASE_URL;

        private ApiKey apiKey;

        private String workSpaceId;

        private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

        private String completionsPath = TEXT_GENERATION_RESTFUL_URL;

        private String embeddingsPath = TEXT_EMBEDDING_RESTFUL_URL;

        private String rerankPath = TEXT_RERANK_RESTFUL_URL;

        private RestClient.Builder restClientBuilder = createDefaultRestClientBuilder();

        private WebClient.Builder webClientBuilder = createDefaultWebClientBuilder();

        private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

        public Builder() {
        }



        private static RestClient.Builder createDefaultRestClientBuilder() {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(60 * 1000);
            requestFactory.setReadTimeout(3 * 60 * 1000);
            return RestClient.builder().requestFactory(requestFactory);
        }

        /**
         * Creates a default WebClient.Builder with configured Reactor Netty connection pool.
         * Falls back to simple WebClient.builder() if Reactor Netty classes are not available.
         */
        private static WebClient.Builder createDefaultWebClientBuilder() {
            try {
                // Try to use Reactor Netty connection pool if available
                Class<?> connectionProviderClass = Class.forName("reactor.netty.resources.ConnectionProvider");
                Class<?> httpClientClass = Class.forName("reactor.netty.http.client.HttpClient");
                Class<?> reactorConnectorClass = Class.forName("org.springframework.http.client.reactive.ReactorClientHttpConnector");

                // Build ConnectionProvider
                Object providerBuilder = connectionProviderClass.getMethod("builder", String.class)
                        .invoke(null, "dashscope-connection-provider");
                // Actively close connections after 45 seconds of idle time to avoid reusing connections that have been closed by the server
                providerBuilder = providerBuilder.getClass().getMethod("maxConnections", int.class)
                        .invoke(providerBuilder, 500);
                providerBuilder = providerBuilder.getClass().getMethod("maxIdleTime", Duration.class)
                        .invoke(providerBuilder, Duration.ofSeconds(45));
                providerBuilder = providerBuilder.getClass().getMethod("maxLifeTime", Duration.class)
                        .invoke(providerBuilder, Duration.ofMinutes(10));
                providerBuilder = providerBuilder.getClass().getMethod("pendingAcquireTimeout", Duration.class)
                        .invoke(providerBuilder, Duration.ofSeconds(60));
                providerBuilder = providerBuilder.getClass().getMethod("evictInBackground", Duration.class)
                        .invoke(providerBuilder, Duration.ofSeconds(30));
                Object provider = providerBuilder.getClass().getMethod("build").invoke(providerBuilder);

                // Build HttpClient
                Object httpClient = httpClientClass.getMethod("create", connectionProviderClass)
                        .invoke(null, provider);
                httpClient = httpClientClass.getMethod("compress", boolean.class).invoke(httpClient, true);
                httpClient = httpClientClass.getMethod("keepAlive", boolean.class).invoke(httpClient, true);
                httpClient = httpClientClass.getMethod("responseTimeout", Duration.class)
                        .invoke(httpClient, Duration.ofSeconds(60));

                // Create ReactorClientHttpConnector
                Object connector = reactorConnectorClass.getConstructor(httpClientClass)
                        .newInstance(httpClient);

                // Build WebClient with connector
                return WebClient.builder()
                        .clientConnector((ClientHttpConnector) connector);
            }
            catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Reactor Netty classes not available, fall back to simple builder
                return WebClient.builder();
            }
            catch (Exception e) {
                // Any other exception, fall back to simple builder
                return WebClient.builder();
            }
        }

        public ContextualDashScopeApi.Builder baseUrl(String baseUrl) {

            Assert.notNull(baseUrl, "Base URL cannot be null");
            this.baseUrl = baseUrl;
            return this;
        }

        public ContextualDashScopeApi.Builder workSpaceId(String workSpaceId) {
            // Workspace ID is optional, but if provided, it must not be null.
            if (StringUtils.hasText(workSpaceId)) {
                Assert.notNull(workSpaceId, "Workspace ID cannot be null");
            }
            this.workSpaceId = workSpaceId;
            return this;
        }

        public ContextualDashScopeApi.Builder apiKey(String simpleApiKey) {
            Assert.notNull(simpleApiKey, "Simple api key cannot be null");
            this.apiKey = new SimpleApiKey(simpleApiKey);
            return this;
        }

        public ContextualDashScopeApi.Builder headers(MultiValueMap<String, String> headers) {
            Assert.notNull(headers, "Headers cannot be null");
            this.headers = headers;
            return this;
        }

        public ContextualDashScopeApi.Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            Assert.notNull(restClientBuilder, "Rest client builder cannot be null");
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        public ContextualDashScopeApi.Builder completionsPath(String completionsPath) {
            Assert.hasText(completionsPath, "completionsPath cannot be null");
            this.completionsPath = completionsPath;
            return this;
        }

        public ContextualDashScopeApi.Builder embeddingsPath(String embeddingsPath) {
            Assert.hasText(embeddingsPath, "embeddingsPath cannot be null");
            this.embeddingsPath = embeddingsPath;
            return this;
        }

        public ContextualDashScopeApi.Builder rerankPath(String rerankPath) {
            Assert.hasText(rerankPath, "rerankPath cannot be null");
            this.rerankPath = rerankPath;
            return this;
        }

        public ContextualDashScopeApi.Builder webClientBuilder(WebClient.Builder webClientBuilder) {
            Assert.notNull(webClientBuilder, "Web client builder cannot be null");
            this.webClientBuilder = webClientBuilder;
            return this;
        }

        public ContextualDashScopeApi.Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
            Assert.notNull(responseErrorHandler, "Response error handler cannot be null");
            this.responseErrorHandler = responseErrorHandler;
            return this;
        }

        public ContextualDashScopeApi build() {
            Assert.notNull(apiKey, "API key cannot be null");

            return new ContextualDashScopeApi(this.baseUrl, this.apiKey, this.headers, this.workSpaceId,
                    this.completionsPath, this.embeddingsPath, this.rerankPath,
                    this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
        }
    }

    private void addDefaultHeadersIfMissing(HttpHeaders headers) {

        if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && !(this.apiKey instanceof NoopApiKey)) {
            headers.setBearerAuth(this.apiKey.getValue());
        }
    }

    @Override
    public Flux<DashScopeApiSpec.ChatCompletionChunk> chatCompletionStream(DashScopeApiSpec.ChatCompletionRequest chatRequest, MultiValueMap<String, String> additionalHttpHeader) {
        ModelCredentials modelCredentials = ModelContextHolder.get();
        if (null != modelCredentials) {
            String apiKey = modelCredentials.getApiKey();
            String model = modelCredentials.getModel();
            Boolean multiModel = modelCredentials.getMultiModel();
            chatRequest = new DashScopeApiSpec.ChatCompletionRequest(chatRequest.model(), chatRequest.input(), chatRequest.parameters(), chatRequest.stream(), multiModel);
            if (StrUtil.isNotBlank(apiKey)) {
                additionalHttpHeader.add(HttpHeaders.AUTHORIZATION, apiKey);
            }
            if (StrUtil.isNotBlank(model)) {
                chatRequest = new DashScopeApiSpec.ChatCompletionRequest(model, chatRequest.input(), chatRequest.parameters(), chatRequest.stream(), multiModel);
            }
        }

        Assert.notNull(chatRequest, "The request body can not be null.");
        Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

        AtomicBoolean isInsideTool = new AtomicBoolean(false);
        boolean incrementalOutput = chatRequest.parameters() != null
                && chatRequest.parameters().incrementalOutput() != null && chatRequest.parameters().incrementalOutput();
        DashScopeAiStreamFunctionCallingHelper chunkMerger = new DashScopeAiStreamFunctionCallingHelper(
                incrementalOutput);

        String chatCompletionUri = this.completionsPath;
        if (modelCredentials != null && StrUtil.isNotBlank(modelCredentials.getCompletionsPath())) {
            chatCompletionUri = modelCredentials.getCompletionsPath();
        }
        if (Boolean.TRUE.equals(chatRequest.multiModel())) {
            chatCompletionUri = MULTIMODAL_GENERATION_RESTFUL_URL;
        }

        WebClient threadWebClient = this.webClient;
        if (modelCredentials != null && StrUtil.isNotBlank(modelCredentials.getBaseUrl())) {
            threadWebClient = webClientBuilder.clone()
                    .baseUrl(modelCredentials.getBaseUrl())
                    .defaultHeaders(finalHeaders)
                    .build();
        }

        return threadWebClient.post().uri(chatCompletionUri).headers(headers -> {
                    headers.addAll(additionalHttpHeader);
                    // For DashScope stream
                    headers.add(HEADER_SSE, ENABLED);
                    addDefaultHeadersIfMissing(headers);
                })
                .body(Mono.just(chatRequest), DashScopeApiSpec.ChatCompletionRequest.class)
                .retrieve()
                .bodyToFlux(String.class)
                .takeUntil(SSE_DONE_PREDICATE)
                .filter(SSE_DONE_PREDICATE.negate())
                .map(content -> {
                    DashScopeApiSpec.DashScopeErrorResponse error = ModelOptionsUtils.jsonToObject(content, DashScopeApiSpec.DashScopeErrorResponse.class);
                    if (error != null && error.code() != null) {
                        throw new DashScopeException(error.code(),String.format("[%s] %s (requestId: %s)",
                                error.code(), error.message(), error.requestId()));
                    }
                    DashScopeApiSpec.ChatCompletionChunk chunk = ModelOptionsUtils.jsonToObject(content, DashScopeApiSpec.ChatCompletionChunk.class);
                    if (chunk == null) {
                        throw new DashScopeException("Failed to parse response content: " + content);
                    }
                    return chunk;
                })
                .map(chunk -> {
                    if (chunkMerger.isStreamingToolFunctionCall(chunk)) {
                        isInsideTool.set(true);
                    }
                    return chunk;
                })
                .windowUntil(chunk -> {
                    if (isInsideTool.get() && chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
                        isInsideTool.set(false);
                        return true;
                    }
                    return !isInsideTool.get();
                })
                .concatMapIterable(window -> {
                    Mono<DashScopeApiSpec.ChatCompletionChunk> monoChunk = window.reduce(
                            new DashScopeApiSpec.ChatCompletionChunk(null, null, null, null),
                            chunkMerger::merge
                    );
                    return List.of(monoChunk);
                })
                .flatMap(mono -> mono);
    }
}
