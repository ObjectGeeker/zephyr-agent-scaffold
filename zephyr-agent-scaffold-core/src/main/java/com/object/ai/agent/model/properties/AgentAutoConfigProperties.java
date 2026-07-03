package com.object.ai.agent.model.properties;

import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "spring.ai.agent")
@Data
@Schema(description = "Agent自动装配属性")
public class AgentAutoConfigProperties {

    @Schema(description = "是否开启自动装配")
    private boolean enabled = true;

    @Schema(description = "自动装配AgentMap")
    private Map<String, AgentConfigTableVO> tableMap;

}
