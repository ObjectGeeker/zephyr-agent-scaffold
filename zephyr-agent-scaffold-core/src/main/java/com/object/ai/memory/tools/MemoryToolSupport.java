package com.object.ai.memory.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.object.ai.memory.constants.MemoryMetadataKeys;
import com.object.ai.memory.model.enums.MessageRoleEnum;
import com.object.ai.memory.model.po.MessagePO;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 记忆 Tool 公共逻辑：从 ToolContext 解析会话上下文、格式化历史消息。
 */
final class MemoryToolSupport {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;
    static final int MAX_CONTENT_LENGTH = 800;

    private MemoryToolSupport() {
    }

    static Optional<RunnableConfig> resolveConfig(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return Optional.empty();
        }
        Object configObj = toolContext.getContext().get(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY);
        if (configObj instanceof RunnableConfig runnableConfig) {
            return Optional.of(runnableConfig);
        }
        return Optional.empty();
    }

    static Optional<String> resolveSessionId(ToolContext toolContext) {
        return resolveConfig(toolContext).flatMap(RunnableConfig::threadId);
    }

    static Optional<String> resolveUserId(ToolContext toolContext) {
        return resolveConfig(toolContext)
                .flatMap(config -> config.metadata(MemoryMetadataKeys.USER_ID, new TypeRef<String>() {
                }));
    }

    static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    static String formatHistory(List<MessagePO> messages, Integer oldestIndex, Integer newestIndex, int totalCount) {
        if (messages.isEmpty()) {
            return "当前会话没有可查询的历史消息。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(totalCount).append(" 条历史消息，本次返回 ")
                .append(messages.size()).append(" 条");
        if (oldestIndex != null && newestIndex != null) {
            sb.append("（序号 ").append(oldestIndex).append(" ~ ").append(newestIndex).append("）");
        }
        sb.append("。\n");
        if (oldestIndex != null && oldestIndex > 0) {
            sb.append("如需更早记录，可使用 beforeIndex=").append(oldestIndex).append(" 翻页。\n");
        }
        sb.append("\n");

        String body = messages.stream()
                .map(MemoryToolSupport::formatMessage)
                .collect(Collectors.joining("\n"));
        sb.append(body);
        return sb.toString();
    }

    private static String formatMessage(MessagePO message) {
        String roleLabel = toRoleLabel(message.getRole());
        String content = truncate(message.getMessageContent());
        return "[" + message.getMessageIndex() + "] " + roleLabel + ": " + content;
    }

    private static String toRoleLabel(String role) {
        if (MessageRoleEnum.user.name().equals(role)) {
            return "用户";
        }
        if (MessageRoleEnum.assistant.name().equals(role)) {
            return "助手";
        }
        if (MessageRoleEnum.tool.name().equals(role)) {
            return "工具";
        }
        if (MessageRoleEnum.system.name().equals(role)) {
            return "系统";
        }
        return role;
    }

    private static String truncate(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH) + "...";
    }
}
