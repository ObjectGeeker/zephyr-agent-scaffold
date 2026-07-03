package com.object.ai.agent.service.assembly.factory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.node.AgentRootNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DefaultAgentAssemblyFactory {

    @Resource
    private AgentRootNode agentRootNode;

    public AbstractAgentAssemblySupport getRootNode() {
        return agentRootNode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "动态上下文")
    public static class DynamicContext extends cn.bugstack.wrench.design.framework.tree.DynamicContext {

        @Schema(description = "api")
        private OpenAiApi openAiApi;

        @Schema(description = "阿里百炼api")
        private DashScopeApi dashScopeApi;

        @Schema(description = "chatModel")
        private ChatModel chatModel;

        @Schema(description = "agent key map")
        private Map<String, Agent> agentMap = new HashMap<>();

        @Schema(description = "当前正在组装哪个多Agent")
        private AtomicInteger currentMultiAgentIndex = new AtomicInteger(0);

        @Schema(description = "当前正在组装的多Agent")
        private AgentConfigTableVO.Module.MultiAgent currentMultiAgent;

        @Schema(description = "数据map")
        private Map<String, Object> dataObject;

    }

}
