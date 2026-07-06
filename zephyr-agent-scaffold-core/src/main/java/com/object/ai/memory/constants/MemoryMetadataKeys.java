package com.object.ai.memory.constants;

/**
 * Agent 记忆 Hook 使用的 RunnableConfig 元数据键
 */
public final class MemoryMetadataKeys {

    private MemoryMetadataKeys() {
    }

    public static final String SAVE_MESSAGE = "save_message";

    public static final String FILE_IDS = "file_ids";

    /**
     * 当前登录用户 ID，供记忆 Tool 做会话归属校验。
     */
    public static final String USER_ID = "user_id";
}
