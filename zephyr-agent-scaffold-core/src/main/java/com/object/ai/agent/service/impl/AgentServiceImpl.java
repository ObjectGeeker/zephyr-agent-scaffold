package com.object.ai.agent.service.impl;

import com.object.ai.agent.model.properties.AgentAutoConfigProperties;
import com.object.ai.agent.model.response.AgentInfoDTO;
import com.object.ai.agent.model.valobj.AgentConfigTableVO;
import com.object.ai.agent.service.AgentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    @Resource
    private AgentAutoConfigProperties properties;

    @Override
    public List<AgentInfoDTO> findAgentList() {
        Map<String, AgentConfigTableVO> tableMap = properties.getTableMap();
        return tableMap.values().stream().map(table -> {
            AgentConfigTableVO.Agent agent = table.getAgent();
            return AgentInfoDTO.builder()
                    .agentId(agent.getAgentId())
                    .agentName(agent.getAgentName())
                    .description(agent.getDescription())
                    .build();
        }).collect(Collectors.toList());
    }
}
