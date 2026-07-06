package com.object.ai.memory.hooks;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@HookPositions(value = {HookPosition.BEFORE_MODEL})
public class MessageWindowHook extends MessagesModelHook {

    private static final int MAX_MESSAGES = 10;

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        if (messages.size() <= MAX_MESSAGES) {
            return new AgentCommand(messages);
        }
        // 保留 SystemMessage + 最近 N 条消息
        List<Message> kept = new ArrayList<>();
        // 第一个 SystemMessage 始终保留
        messages.stream()
                .filter(m -> m instanceof SystemMessage)
                .findFirst()
                .ifPresent(kept::add);
        // 追加最近的消息
        int start = messages.size() - MAX_MESSAGES;
        kept.addAll(messages.subList(Math.max(start, 0), messages.size()));
        // 去重（防止 SystemMessage 被重复加入）
        return new AgentCommand(kept.stream().distinct().toList(), UpdatePolicy.REPLACE);
    }

    @Override
    public AgentCommand afterModel(List<Message> messages, RunnableConfig config) {
        return new AgentCommand(messages); // 不修改
    }

    @Override
    public String getName() {
        return "MessageWindowHook";
    }
}
