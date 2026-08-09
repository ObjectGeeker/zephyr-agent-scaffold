package com.object.ai.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 配置表定义
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentConfigTableVO {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 智能体配置
     */
    private Agent agent;

    /**
     * 智能体模块
     */
    private Module module;

    /**
     * 最终运行的智能体名称
     */
    private Runner runner;

    @Data
    public static class Agent {

        /**
         * 智能体ID
         */
        private String agentId;

        /**
         * 智能体名称
         */
        private String agentName;

        /**
         * 智能体描述
         */
        private String agentDesc;

    }

    @Data
    public static class Module {

        private ChatModel chatModel;

        private List<Agent> agents;

        private List<MultiAgent> multiAgents;

        @Data
        public static class ChatModel {
            private String model;
            /**
             * LangChain4j OpenAI-compatible API 根地址，例如 https://host/compatible-mode/v1。
             */
            private String baseUrl;
            private String apiKey;
            /**
             * 旧配置兼容字段。LangChain4j 不接收完整 completionsPath，装配时只取其 API 根路径。
             */
            @Deprecated
            private String completionsPath = "/v1/chat/completions";
            /**
             * 旧配置兼容字段；当前模型装配不使用 embeddingsPath。
             */
            @Deprecated
            private String embeddingsPath = "/v1/embeddings";
        }

        @Data
        public static class Agent {
            private String name;
            private String instruction;
            private String description;
            private String outputKey;

            private List<ToolMcp> toolMcpList;

            private List<ToolSkills> toolSkillsList;

            /**
             * 工具定义到Agent下，由Adk进行工具调用
             */
            @Data
            public static class ToolMcp {

                private SSEServerParameters sse;

                private StreamableHttpServerParameters streamableHttp;

                private StdioServerParameters stdio;

                private FunctionCall functionCall;

                @Data
                public static class SSEServerParameters {
                    private String name;
                    private String baseUri;
                    private String sseEndpoint;
                    private Integer requestTimeout = 3000;
                }

                @Data
                public static class StreamableHttpServerParameters {
                    private String name;
                    private String url;
                    private Map<String, String> headers;
                    private Integer requestTimeout = 3000;
                    private Integer readTimeout = 3000;
                    private Boolean terminateOnClose = true;
                }

                @Data
                public static class StdioServerParameters {
                    private String name;
                    private Integer requestTimeout = 3000;
                    private ServerParameters serverParameters;

                    @Data
                    public static class ServerParameters {
                        private String command;
                        private List<String> args;
                        private Map<String, String> env;
                    }
                }

                @Data
                public static class FunctionCall {
                    /**
                     * 全类名,自动扫描类下的方法
                     */
                    private String fullClassName;
                }
            }

            @Data
            public static class ToolSkills {
                /**
                 * 类型；directory（用户配置的，映射进来的）、resource（放到工程下的）
                 */
                private String type = "directory";
                /**
                 * 路径；
                 */
                private String path;
            }
        }

        /**
         * 多 Agent 定义
         */
        @Data
        public static class MultiAgent {
            /**
             * 类型；loop、parallel、sequential
             */
            private String type;
            private String name;
            private List<String> subAgents;
            private String description;
            private Integer maxIterations = 3;

        }
    }

    @Data
    public static class Runner {
        private String agentName;
        private List<String> pluginNameList;
    }

}
