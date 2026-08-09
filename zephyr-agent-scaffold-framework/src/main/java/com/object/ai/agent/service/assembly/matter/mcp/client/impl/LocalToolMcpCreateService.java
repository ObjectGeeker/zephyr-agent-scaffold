package com.object.ai.agent.service.assembly.matter.mcp.client.impl;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import com.google.adk.tools.Annotations;
import com.google.adk.tools.FunctionTool;
import com.object.ai.agent.model.valobj.AiAgentConfigTableVO;
import com.object.ai.agent.service.assembly.matter.mcp.client.ToolMcpCreateService;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Component
public class LocalToolMcpCreateService implements ToolMcpCreateService<FunctionTool> {
    @Override
    public List<FunctionTool> buildToolCallBack(AiAgentConfigTableVO.Module.Agent.ToolMcp toolMcp) {
        AiAgentConfigTableVO.Module.Agent.ToolMcp.FunctionCall functionCall = toolMcp.getFunctionCall();
        String fullClassName = functionCall.getFullClassName();
        //1. 根据全类名获取class
        try {
            ClassLoader classLoader = ClassUtil.getClassLoader();
            Class<?> clazz = classLoader.loadClass(fullClassName);
            //2. 扫描类下的方法
            Method[] methods = ReflectUtil.getMethods(clazz);
            //3. 获取带有注解的方法
            List<FunctionTool> functionTools = new ArrayList<>();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(Annotations.Schema.class)) {
                    continue;
                }
                functionTools.add(FunctionTool.create(method));
            }
            return functionTools;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
