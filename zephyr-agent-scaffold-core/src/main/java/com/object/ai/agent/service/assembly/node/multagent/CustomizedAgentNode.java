package com.object.ai.agent.service.assembly.node.multagent;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.object.ai.agent.model.valobj.AgentAssemblyCommandVO;
import com.object.ai.agent.model.valobj.AgentAssemblyRegisterVO;
import com.object.ai.agent.service.assembly.AbstractAgentAssemblySupport;
import com.object.ai.agent.service.assembly.factory.DefaultAgentAssemblyFactory;
import com.object.ai.agent.service.assembly.node.MultiAgentNode;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;

@Component
public class CustomizedAgentNode extends AbstractAgentAssemblySupport {

    @Override
    protected AgentAssemblyRegisterVO doApply(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        log.debug("agent auto assembly customized agent node start");

        buildMultiAgent(dynamicContext, (currentAgent, subAgents) -> {
            try {
                Class<CustomizedAgentArmory> objectClass = ClassUtil.loadClass(dynamicContext.getCurrentMultiAgent().getCustomizedClassName());
                Constructor<CustomizedAgentArmory> constructor = objectClass.getConstructor();
                CustomizedAgentArmory customizedAgentArmory = constructor.newInstance();
                return customizedAgentArmory.buildCustomizedAgent(currentAgent, subAgents);
            } catch (Exception e) {
                log.error("agent auto assembly customized agent node error", e);
                return null;
            }
        });

        dynamicContext.getCurrentMultiAgentIndex().incrementAndGet();

        log.debug("agent auto assembly customized agent node end");
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AgentAssemblyCommandVO, DefaultAgentAssemblyFactory.DynamicContext, AgentAssemblyRegisterVO> get(AgentAssemblyCommandVO requestParameter, DefaultAgentAssemblyFactory.DynamicContext dynamicContext) throws Exception {
        return SpringUtil.getBean(MultiAgentNode.class);
    }
}
