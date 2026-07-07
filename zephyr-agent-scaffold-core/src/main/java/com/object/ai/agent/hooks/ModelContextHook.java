package com.object.ai.agent.hooks;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.object.ai.agent.constants.ModelMetadataKeys;
import com.object.ai.agent.model.context.ModelContextHolder;
import com.object.ai.agent.model.context.ModelCredentials;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 在每次模型调用前从 RunnableConfig 恢复 BYOK 凭证到 ThreadLocal，
 * 解决工具循环跨线程时 ModelContextHolder 丢失的问题。
 */
@Component
@HookPositions(value = {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ModelContextHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "model_context_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        config.metadata(ModelMetadataKeys.MODEL_CREDENTIALS, new TypeRef<ModelCredentials>() {
        }).ifPresent(ModelContextHolder::set);
        return new AgentCommand(messages);
    }

    @Override
    public AgentCommand afterModel(List<Message> messages, RunnableConfig config) {
        ModelContextHolder.clear();
        return new AgentCommand(messages);
    }
}
