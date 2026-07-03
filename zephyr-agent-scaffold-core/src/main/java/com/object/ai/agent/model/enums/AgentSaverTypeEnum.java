package com.object.ai.agent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentSaverTypeEnum {
    memory,
    redis,
    mongodb,
    custom
}
