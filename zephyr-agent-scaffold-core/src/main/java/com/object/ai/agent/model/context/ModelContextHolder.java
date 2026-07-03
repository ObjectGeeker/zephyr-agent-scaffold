package com.object.ai.agent.model.context;

public final class ModelContextHolder {
    private static final ThreadLocal<ModelCredentials> HOLDER = new ThreadLocal<>();
    public static void set(ModelCredentials creds) { HOLDER.set(creds); }
    public static ModelCredentials get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}