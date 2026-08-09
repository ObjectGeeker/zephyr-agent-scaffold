package com.object.ai.config;

import cn.hutool.core.collection.CollUtil;
import com.google.gson.Gson;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import com.object.ai.agent.service.AgentAssemblyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
@Slf4j
public class AiAgentAutoConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private AiAgentAutoConfigProperties properties;

    @Resource
    private AgentAssemblyService agentAssemblyService;

    /**
     * 监听SpringBoot启动完成事件
     *
     * @param event 启动完成事件
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ai agent table auto config enable {}", properties.isEnable());
        if (properties.isEnable() && CollUtil.isNotEmpty(properties.getTableMap())) {
            Map<String, AiAgentConfigTableVO> tableMap = properties.getTableMap();
            Gson gson = new Gson();
            String tableMapJsonStr = gson.toJson(tableMap);
            log.info("ai agent table map {}", tableMapJsonStr);
            try {
                agentAssemblyService.doAgentAssembly(CollUtil.newArrayList(tableMap.values()));
            } catch (Exception e) {
                log.error("ai agent assembly error", e);
            }
        }
    }
}
