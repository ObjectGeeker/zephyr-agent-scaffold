package com.object.ai.agent.service.chat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.object.ai.common.cache.LocalCacheManager;
import com.object.ai.agent.model.request.AgentChatRequest;
import com.object.ai.agent.model.request.AgentSessionCreateRequest;
import com.object.ai.agent.model.response.AgentStreamChatResponse;
import com.object.ai.agent.model.response.AgentSessionCreateResponse;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import com.object.ai.agent.service.AgentChatService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentChatServiceImpl implements AgentChatService {

    /**
     * 本地 Guava 缓存：缓存已经从 ADK SessionService 读取过的 Session 快照。
     *
     * <p>它不是会话的最终存储，真正的会话仍由 Runner 内部的 SessionService 管理。
     * 缓存过期或应用重启后，可以再次从 SessionService 读取；不会因为缓存未命中而重新创建会话。
     */
    private static final LocalCacheManager<AgentSessionCacheKey, Session> SESSION_CACHE =
            new LocalCacheManager<>(1000, Duration.ofMinutes(30));

    @Override
    public AgentSessionCreateResponse createSession(AgentSessionCreateRequest request) {
        validateSessionRequest(request.getAgentId(), request.getUserId());

        // 创建接口只在“新建对话”时调用一次。
        AiAgentRegisterVO registerVO = getRegisterVO(request.getAgentId());
        Runner runner = registerVO.getRunner();
        Session session = runner
                .sessionService()
                .createSession(runner.appName(), request.getUserId())
                .blockingGet();

        // ADK 创建成功后，把 Session 放入 Guava 本地缓存，供后续 chat/stream 快速读取。
        SESSION_CACHE.put(
                new AgentSessionCacheKey(request.getAgentId(), request.getUserId(), session.id()),
                session);

        return AgentSessionCreateResponse.builder()
                .agentId(request.getAgentId())
                .userId(request.getUserId())
                .sessionId(session.id())
                .build();
    }

    @Override
    public List<String> chat(AgentChatRequest agentChatRequest) {
        AiAgentRegisterVO registerVO = getRegisterVO(agentChatRequest.getAgentId());
        Runner runner = registerVO.getRunner();

        // 这里读取的是前端创建好的已有 Session，不再为每条消息创建新会话。
        Session session = getExistingSession(registerVO, agentChatRequest.getAgentId(),
                agentChatRequest.getUserId(), agentChatRequest.getSessionId());

        List<Part> parts = buildParts(agentChatRequest);

        Content content = Content.builder().role("user").parts(parts).build();
        Map<String, Object> customMetaData = buildByokParams(agentChatRequest);
        RunConfig runConfig = RunConfig.builder().customMetadata(customMetaData).build();
        Flowable<Event> eventFlowable = runner.runAsync(session.userId(), session.id(), content, runConfig);

        List<String> outputs = new ArrayList<>();
        eventFlowable.blockingForEach(event -> {
            outputs.add(event.stringifyContent());
        });

        return outputs;

    }

    @NotNull
    private static Map<String, Object> buildByokParams(AgentChatRequest agentChatRequest) {
        Map<String, Object> customMetaData = new HashMap<>();
        if (StrUtil.isNotBlank(agentChatRequest.getModel())) {
            customMetaData.put("model", agentChatRequest.getModel());
        }
        if (StrUtil.isNotBlank(agentChatRequest.getApiKey())) {
            customMetaData.put("apiKey", agentChatRequest.getApiKey());
        }
        if (StrUtil.isNotBlank(agentChatRequest.getBaseUrl())) {
            customMetaData.put("baseUrl", agentChatRequest.getBaseUrl());
        }
        return customMetaData;
    }

    @NotNull
    private static List<Part> buildParts(AgentChatRequest agentChatRequest) {
        List<Part> parts = new ArrayList<>();

        List<AgentChatRequest.Content.Text> texts = agentChatRequest.getTexts();
        if (null != texts && !texts.isEmpty()) {
            for (AgentChatRequest.Content.Text text : texts) {
                parts.add(Part.fromText(text.getMessage()));
            }
        }

        List<AgentChatRequest.Content.File> files = agentChatRequest.getFiles();
        if (null != files && !files.isEmpty()) {
            for (AgentChatRequest.Content.File file : files) {
                parts.add(Part.fromUri(file.getFileUri(), file.getMimeType()));
            }
        }

        List<AgentChatRequest.Content.InlineData> inlineDatas = agentChatRequest.getInlineDatas();
        if (null != inlineDatas && !inlineDatas.isEmpty()) {
            for (AgentChatRequest.Content.InlineData inlineData : inlineDatas) {
                parts.add(Part.fromBytes(inlineData.getBytes(), inlineData.getMimeType()));
            }
        }
        return parts;
    }

    /**
     * 流式对话的整体链路如下：
     *
     * <pre>
     * HTTP 请求
     *   -> Controller 创建 SseEmitter 并调用本方法
     *   -> 本方法立即注册 Flowable 订阅并返回
     *   -> ADK 每产生一个 Event，就转换成一个 AgentStreamChatResponse
     *   -> SseEmitter 将响应作为 SSE 推送给浏览器
     *   -> Flowable 完成或异常，关闭 SSE 请求
     * </pre>
     *
     * <p>这里的关键点是：SseEmitter 负责“把数据写回 HTTP 连接”，Flowable 负责“产生
     * AI 事件”，Disposable 负责“控制这次 Flowable 订阅的生命周期”。三者不是同一个对象。
     */
    @Override
    public void stream(AgentChatRequest agentChatRequest, SseEmitter sseEmitter) {
        // subscribe() 会返回一个 Disposable。
        // 由于 SseEmitter 的回调和 Flowable 的订阅可能在不同时间触发，先用
        // AtomicReference 保存它，后续客户端断开/超时时可以取消订阅。
        AtomicReference<Disposable> disposableReference = new AtomicReference<>();
        try {
            // 根据请求中的 agentId 找到已经装配好的 Agent Runner。
            AiAgentRegisterVO registerVO = getRegisterVO(agentChatRequest.getAgentId());
            Runner runner = registerVO.getRunner();

            // 将文本、文件、图片等输入统一转换为 ADK 能识别的 Part 列表。
            List<Part> parts = buildParts(agentChatRequest);
            Content content = Content.builder().role("user").parts(parts).build();

            // 将用户传入的 model、apiKey、baseUrl 放进本次运行的自定义元数据。
            // 空值不会放入 metadata，底层模型会继续使用 Agent 配置里的默认值。
            Map<String, Object> customMetaData = buildByokParams(agentChatRequest);

            // SSE 模式告诉 ADK：不要等完整答案生成后再返回，而是尽可能产生流式 Event。
            RunConfig runConfig = RunConfig.builder()
                    .streamingMode(RunConfig.StreamingMode.SSE)
                    .customMetadata(customMetaData)
                    .build();

            // Flowable.defer 的含义是“等到真正 subscribe 时，才执行里面的代码”。
            // 因此 Session 读取和 runner.runAsync 不会在 Controller 调用本方法时立即执行。
            Flowable<Event> eventFlowable = Flowable.defer(() -> {
                // 读取前端传入的 sessionId 对应的已有会话。
                // 缓存未命中时，getExistingSession 会从 ADK SessionService 读取，绝不会重新 createSession。
                Session session = getExistingSession(registerVO, agentChatRequest.getAgentId(),
                        agentChatRequest.getUserId(), agentChatRequest.getSessionId());

                // runAsync 返回 Flowable<Event>，每个 Event 代表 Agent 执行过程中的一个事件，
                // 可能是文本分片、工具调用、工具返回或最终事件。
                return runner.runAsync(session.userId(), session.id(), content, runConfig);

            // subscribeOn 指定“从哪里开始执行这条流”。Schedulers.io() 是适合网络/IO 等
            // 阻塞操作的线程池，所以 Session 创建和 Agent 执行不会占用 Spring HTTP 工作线程。
            }).subscribeOn(Schedulers.io());

            // HTTP 客户端正常结束时，取消 Flowable 订阅，避免后台任务继续产生无用事件。
            sseEmitter.onCompletion(() -> dispose(disposableReference));

            // SSE 超时时取消订阅。Controller 当前设置的超时时间很长，但仍然需要兜底。
            sseEmitter.onTimeout(() -> dispose(disposableReference));

            // 发生 HTTP/SSE 层错误时取消订阅，例如浏览器关闭连接或网络断开。
            sseEmitter.onError(throwable -> dispose(disposableReference));

            // subscribe 是真正“启动”Flowable 的地方：
            // - 第一个 lambda 是 onNext：每收到一个 Event 就执行一次。
            // - 第二个 lambda 是 onError：流执行失败时执行一次。
            // - 第三个 lambda 是 onComplete：流正常结束时执行一次。
            // subscribe 返回 Disposable，它就是这次订阅的取消句柄。
            Disposable disposable = eventFlowable.subscribe(
                    event -> sendEvent(sseEmitter, registerVO, event),
                    throwable -> {
                        // 这里处理的是 ADK/RxJava 执行过程中的异常，而不是单个业务字段为空。
                        log.error("Agent stream failed, agentId={}", agentChatRequest.getAgentId(), throwable);
                        completeWithError(sseEmitter, throwable);
                    },
                    // 所有 Event 都发送完成后，明确告诉 Spring 关闭 SSE HTTP 连接。
                    sseEmitter::complete
            );

            // 保存 Disposable，供上面注册的 onCompletion/onTimeout/onError 回调使用。
            disposableReference.set(disposable);
        } catch (Exception exception) {
            // 这里捕获的是“订阅还没有成功建立”之前的异常，例如 Agent 不存在、参数构造失败等。
            log.error("Unable to start agent stream, agentId={}", agentChatRequest.getAgentId(), exception);
            completeWithError(sseEmitter, exception);
        }
    }

    private static void sendEvent(SseEmitter sseEmitter, AiAgentRegisterVO registerVO, Event event) {
        // 先提取当前 Event 中真正有展示意义的数据。
        // ADK 可能会产生 content.parts 为空的中间事件，这类事件虽然存在 Event 对象，
        // 但对前端没有任何可展示内容，因此不能直接转换后发送。
        String content = extractContent(event);
        String toolCallName = extractToolCallName(event);
        String toolCallResponse = extractToolCallResponse(event);

        // 当前 DTO 没有错误字段，流级别的异常会在 subscribe 的 onError 中处理。
        // 因此当文本、工具调用名称、工具返回结果全部为空时，直接过滤这个事件。
        // 这样可以避免前端收到大量类似：
        // {"content":"","toolCallName":"","toolCallResponse":""}
        if (StrUtil.isBlank(content)
                && StrUtil.isBlank(toolCallName)
                && StrUtil.isBlank(toolCallResponse)) {
            return;
        }

        // ADK 的 Event 不是直接暴露给前端，而是转换成项目定义的响应体。
        AgentStreamChatResponse response = AgentStreamChatResponse.builder()
                // Event.author() 通常是实际产生事件的 Agent 名称；为空时使用注册信息兜底。
                .agentName(StrUtil.isNotBlank(event.author()) ? event.author() : registerVO.getAgentName())
                // 文本分片放到 content，前端收到多个 SSE 后按顺序拼接即可得到完整答案。
                .content(content)
                // 工具调用和工具返回使用响应体中专门的字段，避免和普通文本混在一起。
                .toolCallName(toolCallName)
                .toolCallResponse(toolCallResponse)
                .build();
        try {
            // 一个 ADK Event 对应一个名为 message 的 SSE 事件。
            // data(response, APPLICATION_JSON) 会把 AgentStreamChatResponse 序列化成 JSON，
            // 浏览器收到的内容类似：event: message\ndata: {"agentName":"...", ...}
            sseEmitter.send(SseEmitter.event()
                    .name("message")
                    .data(response, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            // 客户端断开时，写 HTTP 连接可能抛 IOException；转换成 RuntimeException，
            // 让 RxJava 进入 onError 分支，统一完成日志记录和资源清理。
            throw new UncheckedIOException("Unable to send agent stream event", exception);
        }
    }

    private static String extractContent(Event event) {
        // Event.content() 和 Content.parts() 都是 Optional，缺少内容时返回空列表。
        // 只提取 Part 中的文本，并按原始顺序直接拼接，不能在每个分片之间额外加换行。
        return event.content()
                .flatMap(Content::parts)
                .orElse(List.of())
                .stream()
                .map(part -> part.text().orElse(""))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining());
    }

    private static String extractToolCallName(Event event) {
        // 一个 Event 可能包含多个工具调用，因此用英文逗号拼接工具名称。
        return event.functionCalls()
                .stream()
                .map(functionCall -> functionCall.name().orElse(""))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private static String extractToolCallResponse(Event event) {
        // 工具返回内容通常是 Map；这里转成字符串放入 DTO，多个工具返回用换行分隔。
        return event.functionResponses()
                .stream()
                .map(functionResponse -> functionResponse.response()
                        .map(response -> String.valueOf(response))
                        .orElse(""))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n"));
    }

    private static void dispose(AtomicReference<Disposable> disposableReference) {
        // Disposable 可以理解为“这次订阅的停止按钮”。
        // dispose() 会取消后续事件消费，防止客户端已经离开后后台仍然继续处理/发送。
        Disposable disposable = disposableReference.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private static void completeWithError(SseEmitter sseEmitter, Throwable throwable) {
        // completeWithError 会让 Spring 以异常结束当前 SSE 请求。
        // SSE 已经开始发送后，通常不能再修改 HTTP 状态码，所以前端需要把连接异常当作失败处理。
        try {
            sseEmitter.completeWithError(throwable);
        } catch (IllegalStateException ignored) {
            log.debug("SSE emitter was already completed", ignored);
        }
    }

    private static AiAgentRegisterVO getRegisterVO(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        return SpringUtil.getBean("AGENT_" + agentId, AiAgentRegisterVO.class);
    }

    private static void validateSessionRequest(String agentId, String userId) {
        if (StrUtil.isBlank(agentId)) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }

    private static Session getExistingSession(AiAgentRegisterVO registerVO,
                                              String agentId,
                                              String userId,
                                              String sessionId) {
        validateSessionRequest(agentId, userId);
        if (StrUtil.isBlank(sessionId)) {
            throw new IllegalArgumentException("sessionId must not be blank; create a session first");
        }

        AgentSessionCacheKey cacheKey = new AgentSessionCacheKey(agentId, userId, sessionId);

        // LocalCacheManager.get 的 loader 只有在 Guava 缓存未命中时才会执行。
        // 缓存命中：直接复用已有 Session 快照。
        // 缓存未命中：从 ADK SessionService 查询已有会话，并重新放入 Guava 缓存。
        return SESSION_CACHE.get(cacheKey, () -> {
            Session session = registerVO.getRunner()
                    .sessionService()
                    .getSession(registerVO.getRunner().appName(), userId, sessionId, Optional.empty())
                    .blockingGet();
            if (session == null) {
                throw new IllegalArgumentException("session not found: " + sessionId);
            }
            return session;
        });
    }

    /**
     * 使用 agentId、userId、sessionId 共同作为缓存 key，避免不同用户或不同 Agent 复用同一个 ID。
     */
    private record AgentSessionCacheKey(String agentId, String userId, String sessionId) {
    }
}
