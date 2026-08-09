package com.object.ai.agent.service;

import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

public interface AgentAssemblyService {

    /**
     * 执行Agent自动装配
     *
     * @param tableList 自动装配配置表
     */
    void doAgentAssembly(List<AiAgentConfigTableVO> tableList) throws Exception;

}
