package com.object.ai.agent.model.valobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Agent自动装配参数
 *
 * @author object
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent自动装配参数")
public class AgentConfigTableVO implements Serializable {

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用类型: openai、dashscope")
    private String appType;

    @Schema(description = "Agent属性")
    private Agent agent;

    @Schema(description = "Agent模块信息")
    private Module module;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Agent属性")
    public static class Agent {

        @Schema(description = "Agent ID")
        private String agentId;

        @Schema(description = "Agent名称")
        private String agentName;

        @Schema(description = "Agent描述")
        private String description;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Agent模块信息")
    public static class Module {

        @Schema(description = "Api信息")
        private AgentApi api;

        @Schema(description = "对话模型信息")
        private AgentChatModel chatModel;

        @Schema(description = "基础Agent节点信息")
        private List<AgentNode> agentNodes;

        @Schema(description = "多Agent协作信息")
        private List<MultiAgent> multiAgents;

        @Schema(description = "Agent执行信息")
        private AgentRunner agentRunner;


        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Schema(description = "Api信息")
        public static class AgentApi {
            @Schema(description = "密钥")
            private String apiKey;
            @Schema(description = "模型地址")
            private String baseUrl;
            @Schema(description = "模型对话地址")
            private String completionsPath = "/v1/chat/completions";
            @Schema(description = "模型向量地址")
            private String embeddingsPath = "/v1/embeddings";
            @Schema(description = "多模态输入地址")
            private String multiModelPath = "/v1/responses";
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Schema(description = "对话模型信息")
        public static class AgentChatModel {

            @Schema(description = "模型")
            private String model;

        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Schema(description = "基础Agent节点信息")
        public static class AgentNode {

            @Schema(description = "节点Key 全局唯一")
            private String key;

            @Schema(description = "节点名称")
            private String name;

            @Schema(description = "节点描述")
            private String description;

            @Schema(description = "系统提示词")
            private String systemPrompt;

            @Schema(description = "模板指令，支持变量")
            private String instruction;

            @Schema(description = "上下文存储类型")
            private String saverType;

            @Schema(description = "自定义的上下文存储类型")
            private String saverClass;

            @Schema(description = "输出存储的 State Key")
            private String outputKey;

            @Schema(description = "JSON Schema 约束输出格式")
            private String outputSchema;

            @Schema(description = "使用 Java 类自动转换")
            private String outputType;

            @Schema(description = "并行执行工具")
            private Boolean parallelToolExecution = false;

            @Schema(description = "最大并发工具数")
            private Integer maxParallelTools = 1;

            @Schema(description = "工具执行最大时间")
            private Integer toolExecutionTimeout;

            @Schema(description = "是否开启推理过程")
            private Boolean returnReasoningContents = false;

            @Schema(description = "钩子，这里输入beanName")
            private List<String> hooks;

            @Schema(description = "拦截器，这里输入beanName")
            private List<String> interceptors;

            @Schema(description = "工具调用信息")
            private List<ToolMcp> toolMcpList;

            @Schema(description = "技能信息")
            private List<ToolSkills> toolSkills;

            @Data
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            @Schema(description = "工具调用信息")
            public static class ToolMcp {

                @Schema(description = "sse工具调用")
                private SSEServerParameters sse;

                @Schema(description = "stdio工具调用")
                private StdioServerParameters stdio;

                @Schema(description = "本地工具调用")
                private ToolFunctionCall local;

                @Data
                @Builder
                @AllArgsConstructor
                @NoArgsConstructor
                @Schema(description = "sse工具调用")
                public static class SSEServerParameters {

                    @Schema(description = "工具名称")
                    private String name;

                    @Schema(description = "工具地址")
                    private String baseUri;

                    @Schema(description = "地址参数")
                    private String sseEndpoint;

                    @Schema(description = "超时时间")
                    private Integer requestTimeout = 3000;

                }

                @Data
                @Builder
                @AllArgsConstructor
                @NoArgsConstructor
                @Schema(description = "stdio工具调用")
                public static class StdioServerParameters {

                    @Schema(description = "工具名称")
                    private String name;

                    @Schema(description = "超时时间")
                    private Integer requestTimeout = 3000;

                    @Schema(description = "工具参数")
                    private ServerParameters serverParameters;


                    @Data
                    @Builder
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @Schema(description = "stdio参数")
                    public static class ServerParameters {

                        @Schema(description = "命令")
                        private String command;

                        @Schema(description = "参数")
                        private List<String> args;

                        @Schema(description = "环境变量")
                        private Map<String, String> env;

                    }
                }

                /**
                 * 通过Function call方式接入的Tool工具
                 */
                @Data
                @Builder
                @AllArgsConstructor
                @NoArgsConstructor
                @Schema(description = "本地工具调用")
                public static class ToolFunctionCall {
                    /**
                     * 优先使用beanName去容器找，没找到当做全类名通过反射找
                     */
                    @Schema(description = "工具类的bean名称")
                    private String beanName;
                }
            }

            @Data
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            @Schema(description = "技能信息")
            public static class ToolSkills {
                /**
                 * skill文档的类型，可能是一个目录，也可能是一个resource资源文件
                 */
                @Schema(description = "skill类型，文件目录 or resource资源目录")
                private String type = "directory";

                /**
                 * skills文档的存放路径
                 */
                @Schema(description = "skill存放路径")
                private String path;
            }
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Schema(description = "多Agent协作信息")
        public static class MultiAgent {
            @Schema(description = "类型: sequential,loop,parallel,routing,supervisor,customized")
            private String type;
            @Schema(description = "节点key")
            private String key;
            @Schema(description = "名称")
            private String name;
            @Schema(description = "描述，在多Agent协作模式下，描述决定Agent走向")
            private String description;
            @Schema(description = "最大循环次数(loop专用)")
            private Integer maxLoopCount;
            @Schema(description = "自定义Agent协作流程的全类名(customized专用)")
            private String customizedClassName;
            @Schema(description = "子节点的key列表")
            private List<String> subAgentKeys;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Schema(description = "Agent执行信息")
        public static class AgentRunner {

            @Schema(description = "最终运行的AgentKey")
            private String runAgentKey;

        }
    }

}
