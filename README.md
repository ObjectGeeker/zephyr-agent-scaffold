# Zephyr Agent Scaffold

> 一个基于 Google ADK + LangChain4j 的 Java Agent 应用脚手架。

Zephyr Agent Scaffold 1.0 面向需要快速搭建 Agent 应用的 Java/AI 开发者，提供从 Agent 配置、模型接入、工具装配、多 Agent 编排到会话对话和流式输出的一套基础实现。

项目的核心思路是：用 Google ADK 负责 Agent Runtime 和执行事件，用 LangChain4j 负责 ChatModel 适配，再通过 Spring Boot 配置化和装配链把 Agent 组装成可运行的应用。

## 核心亮点

### 1. BYOK：请求级模型配置覆盖

BYOK（Bring Your Own Key）允许调用方在每次对话请求中传入自己的模型配置：

- `model`：模型名称
- `baseUrl`：OpenAI-compatible API 根地址
- `apiKey`：模型服务 API Key

请求中的非空字段会覆盖 Agent YAML 中的默认配置，未传入的字段继续回退到默认值。模型配置通过 ADK 的 `RunConfig.customMetadata` 传递，并由 BYOK 插件写入当前调用上下文；普通调用和 SSE 流式调用都支持该机制。

这使得同一套 Agent 配置可以连接不同的 OpenAI-compatible 服务，也可以让不同用户在请求级别选择自己的模型服务。

> 安全提示：生产环境应谨慎开放 BYOK 能力。不要把 API Key 提交到 Git，也不要在服务端日志中输出完整密钥。示例前端会将设置保存在当前浏览器的 `localStorage` 中，仅适合本地开发体验。

### 2. Google ADK + LangChain4j 组合

项目通过 Google ADK 的 LangChain4j 适配器接入 LangChain4j ChatModel：

```text
请求
  -> ADK Runner
  -> ADK Agent
  -> ADK LangChain4j Adapter
  -> LangChain4j OpenAI-compatible ChatModel
  -> 模型服务
```

当前项目使用 `DynamicLangChain4jChatModel` 同时实现普通和流式 ChatModel，并在真正发起调用时解析当前请求的模型配置。因此，默认模型配置和 BYOK 配置可以共存。

### 3. Agent 配置化

Agent 的应用名称、Agent 元数据、模型、指令、工具、Skills、多 Agent 编排方式和最终运行入口都可以通过 YAML 描述：

```yaml
adk:
  autoconfig:
    enable: true
    table-map:
      common-agent:
        appName: zephyr-agent-scaffold
        agent:
          agentId: agent-001
          agentName: daily-assistant
          agentDesc: 日常聊天助手
        module:
          chatModel:
            model: qwen3.7-plus
            baseUrl: ${LLM_BASE_URL}
            apiKey: ${LLM_API_KEY}
          agents:
            - name: daily-agent
              instruction: 你是一个日常聊天助手。
              description: 日常聊天助手
        runner:
          agent-name: daily-agent
```

应用启动完成后，`adk.autoconfig.table-map` 会被读取并触发 Agent 自动装配。新增或调整 Agent 时，通常只需要修改配置文件和工具定义，不需要修改装配主流程。

### 4. 基于责任链/策略路由的装配

Agent 装配使用策略路由树组织不同阶段，每个节点只负责一种装配职责：

```text
Root
  -> ChatModel
  -> Agent
  -> MultiAgent
      -> Sequential / Parallel / Loop
  -> Runner
```

装配链的主要特点：

- 模型、基础 Agent、多 Agent、Runner 分阶段构建
- 不同多 Agent 类型由独立节点处理
- 节点之间通过动态上下文传递模型、Agent Map 和编排配置
- 最终 Runner 动态注册到 Spring 容器，供对话服务按 `agentId` 获取

这种设计将 Agent 的构建过程拆成可替换、可扩展的策略节点，便于后续加入新的 Agent 类型或装配行为。

## 为什么没有选择 Spring AI？

在实际尝试 Google ADK 与 Spring AI 组合时，主要考虑了以下几点：

- **适配性不够理想**：Google ADK 自身已经提供了 Agent、Runner、Session、Tool 和 Event 等运行时抽象，与 Spring AI 的模型抽象和调用链结合后，边界不够清晰，整合成本较高。
- **版本联动带来的升级成本**：Spring AI 2.0 之后架构发生了较大变化，同时 Google ADK 与 Spring AI 的版本演进存在较强联动。对于脚手架而言，这会增加依赖治理、升级和兼容性维护成本。
- **LangChain4j 更贴合当前接入方式**：LangChain4j 对 OpenAI-compatible API、普通 ChatModel、StreamingChatModel 和工具调用提供了直接支持，适合作为 Google ADK 的模型接入层。
- **保留清晰的职责边界**：当前项目由 Google ADK 负责 Agent Runtime，由 LangChain4j 负责模型适配，由 Spring Boot 负责应用配置和依赖管理，各层职责更明确。

因此，项目最终选择 **Google ADK + LangChain4j + Spring Boot** 的组合：既使用 Google ADK 的 Agent 编排和运行能力，也利用 LangChain4j 的模型接入灵活性，同时降低框架之间的耦合。

## 当前功能

- 单 Agent 配置与运行
- 多 Agent 编排：`sequential`、`parallel`、`loop`
- Google ADK Runner、Session 和 Event 执行链路
- LangChain4j OpenAI-compatible ChatModel 和 StreamingChatModel
- 请求级 BYOK 模型切换
- MCP 工具接入：
  - stdio
  - SSE
  - Streamable HTTP
- Function Call：通过 Java 类全限定名扫描工具方法
- Skills：支持 classpath resource 和本地 directory 两种来源
- 多模态输入：文本、URI 文件、Inline Data（二进制数据）
- Agent 会话创建与复用
- 普通对话和 SSE 流式对话
- 流式事件中的文本、工具调用和工具返回结果拆分输出
- 基于 Guava 的本地会话快照缓存和模型实例缓存
- 内置浏览器聊天页面，支持 Agent 选择、模型选择和 BYOK 设置

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| Web | Spring Boot 3.5.14 |
| Agent Runtime | Google ADK LangChain4j 1.7.1 |
| LLM 抽象与模型接入 | LangChain4j 1.18.1 |
| 模型协议 | OpenAI-compatible API |
| 编排方式 | Google ADK LlmAgent、SequentialAgent、ParallelAgent、LoopAgent |
| 工具接入 | Google ADK Tools、MCP、Function Call、Skills |
| 构建工具 | Maven |

## 项目结构

```text
zephyr-agent-scaffold/
├── zephyr-agent-scaffold-framework/       # Agent 核心框架
│   └── src/main/java/com/object/ai/
│       ├── agent/controller/              # Agent HTTP 接口
│       ├── agent/service/                 # 对话与 Agent 装配服务
│       ├── agent/model/                   # 请求、响应、配置和上下文模型
│       ├── agent/service/assembly/        # 装配链、MCP、BYOK 和动态模型
│       └── common/cache/                  # 本地缓存
├── zephyr-agent-scaffold-app/             # Spring Boot 示例应用
│   └── src/main/resources/
│       ├── agents/                        # Agent YAML 和 Skills
│       └── static/chat/                   # 内置聊天页面
├── pom.xml
└── README.md
```

## 快速开始

### 1. 准备环境

- JDK 21+
- Maven 3.9+
- 一个支持 OpenAI-compatible API 的模型服务

### 2. 配置模型服务

在项目根目录创建 `.env` 文件：

```properties
LLM_BASE_URL=https://your-provider.example.com/v1
LLM_API_KEY=your-api-key
```

`application.yml` 会自动加载根目录或应用目录上级目录下的 `.env` 文件。请使用 OpenAI-compatible API 根地址，不要填写完整的 `/chat/completions` 地址。

默认加载的配置文件是：

```text
zephyr-agent-scaffold-app/src/main/resources/agents/single-agent.yml
```

如需切换配置，可以修改 `application.yml` 中的：

```yaml
spring:
  config:
    import:
      - classpath:agents/single-agent.yml
```

多 Agent 示例位于：

```text
zephyr-agent-scaffold-app/src/main/resources/agents/multi-agent.yml
```

该示例展示了路线规划 Agent 和酒店 Agent 的顺序编排，以及 SSE、stdio MCP 的配置方式。

### 3. 启动应用

```bash
mvn -pl zephyr-agent-scaffold-app -am package -DskipTests
mvn -pl zephyr-agent-scaffold-app spring-boot:run \
  -Dspring-boot.run.main-class=com.object.ai.Application
```

应用默认端口为 `8123`，上下文路径为 `/api`。

内置聊天页面：

```text
http://localhost:8123/api/chat/index.html
```

## HTTP API

### 查询 Agent 列表

```bash
curl -X POST http://localhost:8123/api/agent/query_ai_agent_list
```

### 创建会话

```bash
curl -X POST http://localhost:8123/api/agent/session/create \
  -H 'Content-Type: application/json' \
  -d '{
    "agentId": "agent-001",
    "userId": "user-001"
  }'
```

响应中的 `sessionId` 需要用于后续对话请求。

### 普通对话

```bash
curl -X POST http://localhost:8123/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "agentId": "agent-001",
    "userId": "user-001",
    "sessionId": "SESSION_ID",
    "texts": [
      {"message": "帮我制定今天的工作计划"}
    ]
  }'
```

请求支持文本、URI 文件和 Inline Data：

```json
{
  "texts": [{"message": "请分析这个文件"}],
  "files": [{
    "fileUri": "https://example.com/report.pdf",
    "mimeType": "application/pdf"
  }],
  "inlineDatas": [{
    "bytes": [1, 2, 3],
    "mimeType": "image/png"
  }]
}
```

### SSE 流式对话

```bash
curl -N -X POST http://localhost:8123/api/agent/stream \
  -H 'Content-Type: application/json' \
  -d '{
    "agentId": "agent-001",
    "userId": "user-001",
    "sessionId": "SESSION_ID",
    "texts": [{"message": "请分步骤说明你的分析过程"}]
  }'
```

SSE 事件名为 `message`，响应体包含以下字段：

```json
{
  "agentName": "daily-agent",
  "content": "文本分片",
  "toolCallName": "工具名称",
  "toolCallResponse": "工具返回结果"
}
```

### 请求级 BYOK

在普通或流式对话请求中增加以下字段即可覆盖当前请求的默认模型配置：

```json
{
  "agentId": "agent-001",
  "userId": "user-001",
  "sessionId": "SESSION_ID",
  "model": "qwen-plus",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "apiKey": "your-api-key",
  "texts": [{"message": "你好"}]
}
```

三个字段都不是必填项。未传入或为空的字段会继续使用 Agent YAML 中的默认值。

## Agent 配置说明

Agent 配置对象位于 `adk.autoconfig.table-map` 下，每个配置表包含以下部分：

| 配置节点 | 作用 |
| --- | --- |
| `appName` | ADK 应用名称 |
| `agent` | Agent ID、名称和描述 |
| `module.chatModel` | 默认模型、Base URL 和 API Key |
| `module.agents` | 基础 LlmAgent、指令、输出键和工具 |
| `module.multiAgents` | Sequential、Parallel、Loop 编排定义 |
| `runner` | 指定最终运行的 Agent |

工具可以按配置挂载：

```yaml
toolMcpList:
  - function-call:
      full-class-name: com.object.ai.agent.tools.FrameworkTools
  - streamableHttp:
      name: searchapi-search
      url: https://www.searchapi.io/mcp
      headers:
        X-MCP-Token: ${SEARCHAPI_MCP_TOKEN}
```

Skills 可以使用 classpath 资源：

```yaml
tool-skills-list:
  - type: resource
    path: agents/skills
```

多 Agent 编排示例：

```yaml
multiAgents:
  - type: sequential
    name: travel_planner
    description: 先规划路线，再推荐酒店
    subAgents:
      - route_agent
      - hotel_agent
    maxIterations: 3
```

## 设计说明

### 配置驱动的自动装配

应用监听 Spring Boot `ApplicationReadyEvent`。当 `adk.autoconfig.enable=true` 且存在 `table-map` 时，会遍历配置并构造对应的 ADK Runner，最终以 `AGENT_{agentId}` 的名称注册到 Spring 容器。

### 会话与事件

每个 Agent 会话由 `agentId`、`userId` 和 `sessionId` 共同确定。会话本身由 ADK SessionService 管理，框架使用本地 Guava 缓存保存近期读取的 Session 快照。

普通对话会收集完整 Event 后返回；流式对话使用 RxJava Flowable 和 Spring `SseEmitter`，将 Agent 事件转换为文本、工具调用和工具返回事件持续推送给客户端。

## 版本与边界

这是 Zephyr Agent Scaffold 的 1.0 版本，当前重点是 Agent 基础装配和运行链路。仓库当前使用的关键依赖版本为：

- Google ADK LangChain4j：`1.7.1`
- LangChain4j：`1.18.1`
- Spring Boot：`3.5.14`
- Java：`21`

README 只将当前仓库中已有实现作为已支持能力。知识库、权限认证、微信集成等能力不在当前 1.0 实现范围内。

## License

当前项目未单独声明开源许可证。如需对外发布，请根据实际使用的依赖和发布计划补充 License 文件。
