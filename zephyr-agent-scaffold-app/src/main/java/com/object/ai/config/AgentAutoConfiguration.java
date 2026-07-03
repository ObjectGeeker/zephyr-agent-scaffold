package com.object.ai.config;

import cn.hutool.json.JSONUtil;
import com.object.ai.agent.model.properties.AgentAutoConfigProperties;
import com.object.ai.agent.service.assembly.AgentAssemblyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentAutoConfigProperties.class)
@Slf4j
public class AgentAutoConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private AgentAutoConfigProperties properties;

    @Resource
    private AgentAssemblyService agentAssemblyService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("agent auto armory enable {} table_map {}", properties.isEnabled(), JSONUtil.toJsonStr(properties.getTableMap()));
            agentAssemblyService.assembly(properties);
        } catch (Exception e) {
            log.error("agent auto armory error", e);
        }
    }
}
