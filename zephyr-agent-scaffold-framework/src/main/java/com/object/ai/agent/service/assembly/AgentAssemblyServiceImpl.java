package com.object.ai.agent.service.assembly;

import com.object.ai.agent.model.entity.AgentAssemblyCommandEntity;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.AgentAssemblyService;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.node.AgentAssemblyRootNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class AgentAssemblyServiceImpl implements AgentAssemblyService {

    @Resource
    private DefaultAgentAssemblyFactory defaultAgentAssemblyFactory;

    @Override
    public void doAgentAssembly(List<AiAgentConfigTableVO> tableList) {
        log.info("agent auto assembly size {}", tableList.size());
        for (AiAgentConfigTableVO configTable : tableList) {
            AgentAssemblyRootNode rootNode = defaultAgentAssemblyFactory.rootNode();
            try {
                rootNode.apply(AgentAssemblyCommandEntity.builder().configTable(configTable).build(), new DefaultAgentAssemblyFactory.DynamicContext());
                log.error("agent auto assembly success app name {}", configTable.getAppName());
            } catch (Exception e) {
                log.error("agent auto assembly error app name {} exception", configTable.getAppName(), e);
            }

        }
    }
}
