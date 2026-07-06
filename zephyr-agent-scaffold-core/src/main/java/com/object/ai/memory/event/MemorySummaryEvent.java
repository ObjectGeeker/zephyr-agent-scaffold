package com.object.ai.memory.event;

import lombok.Getter;
import org.springframework.ai.chat.messages.Message;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class MemorySummaryEvent extends ApplicationEvent {
    private final String threadId;
    private final List<Message> messages;
    public MemorySummaryEvent(String threadId, List<Message> messages) {
        super(threadId);
        this.threadId = threadId;
        this.messages = messages;
    }
}
