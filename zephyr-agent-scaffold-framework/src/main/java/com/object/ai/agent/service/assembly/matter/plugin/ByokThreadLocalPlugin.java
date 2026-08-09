package com.object.ai.agent.service.assembly.matter.plugin;

import cn.hutool.core.map.MapUtil;
import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.RunConfig;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.plugins.BasePlugin;
import com.object.ai.agent.model.context.ByokThreadContext;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ByokThreadLocalPlugin extends BasePlugin {
    public ByokThreadLocalPlugin() {
        super("byok_thread_local_plugin");
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest.Builder llmRequest) {
        RunConfig runConfig = callbackContext.invocationContext().runConfig();
        String apiKey = MapUtil.getStr(runConfig.customMetadata(), "apiKey");
        String baseUrl = MapUtil.getStr(runConfig.customMetadata(), "baseUrl");
        String model = MapUtil.getStr(runConfig.customMetadata(), "model");
        ByokThreadContext.set(new ByokThreadContext.ByokConfig(model, baseUrl, apiKey));
        return Maybe.empty();
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        ByokThreadContext.clear();
        return Maybe.empty();
    }
}
