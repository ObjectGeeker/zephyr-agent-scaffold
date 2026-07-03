package com.object.ai.agent.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MultiAgentTypeEnum {
    sequential, loop, parallel, routing, supervisor, customized;
}
