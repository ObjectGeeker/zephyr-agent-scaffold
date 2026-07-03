package com.object.ai.agent.service;

import com.object.ai.agent.model.response.AgentInfoDTO;

import java.util.List;

public interface AgentService {

    /**
     * 查询配置的所有Agent
     *
     * @return 配置的所有Agent
     */
    List<AgentInfoDTO> findAgentList();
}
