package com.object.ai.agent.model.valobj.properties;

import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "adk.autoconfig")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentAutoConfigProperties {

    private boolean enable = false;

    private Map<String, AiAgentConfigTableVO> tableMap;

}
