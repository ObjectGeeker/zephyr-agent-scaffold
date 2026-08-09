package com.object.ai.test;

import cn.hutool.extra.spring.SpringUtil;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.models.langchain4j.LangChain4j;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations;
import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.object.ai.agent.model.valobj.AiAgentRegisterVO;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@SpringBootTest
public class AdkTest {

    @Value("classpath:/static/background.jpg")
    private Resource imageResource;

    @Test
    public void testLlmAgent() {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .modelName("deepseek-v4-flash")
                .baseUrl("")
                .apiKey("")
                .build();
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .modelName("deepseek-v4-flash")
                .baseUrl("")
                .apiKey("")
                .build();
        LangChain4j adkModel = LangChain4j.builder()
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .modelName("deepseek-v4-flash")
                .build();

        FunctionTool tool = FunctionTool.create(AdkTest.class, "getCurrentTime");

        LlmAgent llmAgent = LlmAgent.builder()
                .name("testAgent")
                .model(adkModel)
                .tools(tool)
                .build();


        RunConfig runConfig = RunConfig.builder()
                .streamingMode(RunConfig.StreamingMode.SSE)
                .build();
        InMemoryRunner runner = new InMemoryRunner(llmAgent, "test-app");

        Session session = runner
                .sessionService()
                .createSession(runner.appName(), "user1234")
                .blockingGet();
        Content userMsg = Content.fromParts(Part.fromText("帮我看一下当前系统时间是什么时候"));

        Flowable<Event> eventFlowable = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);
        eventFlowable.blockingForEach(event -> {
            if (event.finalResponse()) {
                System.out.println(event.stringifyContent());
            }
        });
    }

    @Annotations.Schema(name = "getCurrentTime", description = "获取当前系统时间")
    public static String getCurrentTime(@Annotations.Schema(name = "toolContext")
                                 ToolContext toolContext) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(new Date());
    }

    @Test
    public void testAssembly() {
        AiAgentRegisterVO registerVO = SpringUtil.getBean("AGENT_agent-001", AiAgentRegisterVO.class);
        Runner runner = registerVO.getRunner();
        Session session = runner
                .sessionService()
                .createSession(runner.appName(), "user1234")
                .blockingGet();
        Content userMsg = Content.fromParts(Part.fromText("你有什么技能呢"));
        RunConfig runConfig = RunConfig.builder().build();
        Flowable<Event> eventFlowable = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);
        eventFlowable.blockingForEach(event -> {
            if (event.finalResponse()) {
                System.out.println(event.stringifyContent());
            }
        });
    }

    @Test
    public void testPic() throws IOException {
        byte[] contentAsByteArray = imageResource.getContentAsByteArray();
        AiAgentRegisterVO registerVO = SpringUtil.getBean("AGENT_agent-001", AiAgentRegisterVO.class);
        Runner runner = registerVO.getRunner();
        Session session = runner
                .sessionService()
                .createSession(runner.appName(), "user1234")
                .blockingGet();
        Content userMsg = Content.fromParts(Part.fromText("描述一下当前图片"), Part.fromBytes(contentAsByteArray, MimeTypeUtils.IMAGE_JPEG_VALUE));
        RunConfig runConfig = RunConfig.builder().build();
        Flowable<Event> eventFlowable = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);
        eventFlowable.blockingForEach(event -> {
            if (event.finalResponse()) {
                System.out.println(event.stringifyContent());
            }
        });
    }

    @Test
    public void testByokContext() {
        // 仅覆盖 model，baseUrl/apiKey 留空由动态 LangChain4j 模型回退到装配表默认配置。
        // ByokThreadLocalPlugin 会从 customMetadata 设置本次调用的线程上下文。
        AiAgentRegisterVO registerVO = SpringUtil.getBean("AGENT_agent-001", AiAgentRegisterVO.class);
        Runner runner = registerVO.getRunner();
        Session session = runner
                .sessionService()
                .createSession(runner.appName(), "user1234")
                .blockingGet();
        Content userMsg = Content.fromParts(Part.fromText("你是哪个厂商的模型呢"));
        RunConfig runConfig = RunConfig.builder()
                .customMetadata(Map.of("model", "qwen3.8-max"))
                .build();
        Flowable<Event> eventFlowable = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);
        eventFlowable.blockingForEach(event -> {
            if (event.finalResponse()) {
                System.out.println(event.stringifyContent());
            }
        });
    }

}
