# Zephyr Agent Scaffold

基于 Spring Boot、Spring AI Alibaba 和 MyBatis-Plus 的 AI Agent 应用脚手架。项目内置 Web 对话页面、SSE 流式输出、Agent YAML 配置、账号登录注册、微信扫码登录、Sa-Token 登录态、Knife4j/OpenAPI 文档等基础能力，适合作为 Agent 应用的二次开发起点。

## 功能特性

- Agent 对话：支持同步对话和基于 SSE 的流式对话。
- Agent 配置：通过 YAML 配置 Agent、模型、工具、MCP、Multi-Agent 编排等能力。
- 用户认证：支持账号注册、账号登录、微信 openid 登录或注册。
- 登录态管理：基于 Sa-Token 管理登录会话。
- 数据持久化：基于 MyBatis-Plus 和 MySQL 持久化用户数据。
- 前端页面：提供对话页、账号登录/注册页、微信扫码登录页。
- 接口文档：集成 Knife4j 和 SpringDoc OpenAPI。
- 统一响应：使用 `BaseResponse` 和 `ResultUtil` 封装接口返回。

## 技术栈

- Java 17
- Spring Boot 3.5.14
- Spring AI 1.1.7
- Spring AI Alibaba 1.1.2.3
- MyBatis-Plus 3.5.16
- MySQL 9.6.0 Driver
- Sa-Token 1.45.0
- Knife4j 4.4.0
- Hutool
- Lombok
- Redis / Redisson，可按配置关闭
- 前端：原生 HTML、CSS、JavaScript；登录页使用 Tailwind CSS 和 daisyUI CDN

## 模块结构

```text
zephyr-agent-scaffold
├── README.md
├── pom.xml
├── dev-ops
│   └── sql
│       └── init.sql
├── zephyr-agent-scaffold-app
│   ├── src/main/java/com/object/ai/Application.java
│   └── src/main/resources
│       ├── application.properties
│       ├── application-local.properties
│       ├── agent
│       │   ├── only-one-agent.yml
│       │   └── demo-agent.yml
│       └── static
│           ├── index.html
│           ├── login.html
│           └── wechat-login.html
└── zephyr-agent-scaffold-core
    └── src/main/java/com/object/ai
        ├── agent
        ├── auth
        └── common
```

- `zephyr-agent-scaffold-app`：应用启动类、运行配置、静态前端页面和 Agent YAML 配置。
- `zephyr-agent-scaffold-core`：核心业务模块，包含 Agent 对话、用户认证、微信扫码登录、通用异常和统一响应。
- `dev-ops/sql/init.sql`：数据库初始化脚本。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8+
- Redis，可选；默认配置可关闭 Redis
- 微信公众号配置，可选；仅微信扫码登录需要

### 初始化数据库

创建数据库：

```sql
CREATE DATABASE agent_scaffold DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行初始化脚本：

```text
dev-ops/sql/init.sql
```

当前脚本会创建 `tb_user` 用户表，包含账号、密码、微信 openid、角色、状态、昵称、逻辑删除等字段。

### 修改配置

主配置文件位于：

```text
zephyr-agent-scaffold-app/src/main/resources/application.properties
```

重点配置项：

```properties
server.servlet.context-path=/api

spring.datasource.username=root
spring.datasource.password=root
spring.datasource.url=jdbc:mysql://localhost:3306/agent_scaffold?useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&serverTimezone=UTC&useSSL=true

mybatis-plus.mapper-locations=classpath:/mapper/*.xml

springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.group-configs[0].group=default
springdoc.group-configs[0].paths-to-match=/**
springdoc.group-configs[0].packages-to-scan=com.object.ai

wechat.mp.app-id=your-test-app-id
wechat.mp.secret=your-test-app-secret
wechat.mp.token=your-custom-token

redis.config.enable=false
```

`application-local.properties` 可用于本地私有配置覆盖，例如数据库密码、微信 appId、微信 secret、Redis 密码等。不要把真实密钥提交到公共仓库。

### 启动应用

在项目根目录执行：

```bash
mvn -pl zephyr-agent-scaffold-app -am spring-boot:run
```

如果本机没有安装 Maven，先安装 Maven 或为项目补充 Maven Wrapper。

## 页面入口

默认上下文路径是 `/api`。

- 对话页面：`http://localhost:8080/api/index.html`
- 登录/注册页面：`http://localhost:8080/api/login.html`
- 微信扫码登录页面：`http://localhost:8080/api/wechat-login.html`
- Knife4j 文档：`http://localhost:8080/api/doc.html`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/api/v3/api-docs`

## 核心接口

### 用户认证

- `POST /api/auth/register`：账号注册。
- `POST /api/auth/login`：账号登录。
- `POST /api/auth/wechat/login-or-register`：根据微信 openid 登录或注册。

账号注册请求示例：

```json
{
  "registerType": "account",
  "registerAccount": "demo",
  "registerCertificate": "123456",
  "username": "演示用户"
}
```

账号登录请求示例：

```json
{
  "loginType": "account",
  "loginAccount": "demo",
  "loginCertificate": "123456"
}
```

微信 openid 登录或注册请求示例：

```json
{
  "wxOpenid": "wechat-openid"
}
```

### 微信扫码登录

- `POST /api/auth/wechat/qrcode`：创建微信登录二维码。
- `GET /api/auth/wechat/qrcode/{ticketId}`：轮询二维码登录状态。
- `GET /api/wechat/callback`：微信公众号服务器 URL 验证。
- `POST /api/wechat/callback`：微信公众号事件和消息回调。

微信扫码登录流程：

1. 前端调用 `POST /api/auth/wechat/qrcode` 创建二维码。
2. 用户扫码后，公众号回复验证码提示。
3. 用户在公众号中回复验证码。
4. 前端轮询 `GET /api/auth/wechat/qrcode/{ticketId}`。
5. 状态变为 `CONFIRMED` 后，前端调用 `POST /api/auth/wechat/login-or-register` 完成应用登录。

### Agent 对话

`AgentController` 需要登录态，访问前应先完成登录。

- `POST /api/agent/chat`：同步对话，返回完整文本。
- `POST /api/agent/stream`：SSE 流式对话。
- `POST /api/agent/findAgentList`：查询当前配置的 Agent 列表。

流式对话请求示例：

```json
{
  "message": "你好",
  "threadId": "demo-thread",
  "agentId": "agent-001",
  "agentName": "日常聊天助手",
  "model": "deepseek-v4-flash",
  "apiKey": "sk-xxxx",
  "baseUrl": "https://example.com"
}
```

## Agent 配置

当前默认导入：

```properties
spring.config.import=classpath:agent/only-one-agent.yml
```

配置文件位置：

- `zephyr-agent-scaffold-app/src/main/resources/agent/only-one-agent.yml`：当前默认使用的单 Agent 配置。
- `zephyr-agent-scaffold-app/src/main/resources/agent/demo-agent.yml`：更完整的 Agent、MCP、Multi-Agent 配置示例。

`only-one-agent.yml` 的核心结构：

```yaml
spring:
  ai:
    agent:
      table-map:
        only-one-agent:
          app-name: my-ai-application
          app-type: openai
          agent:
            agent-id: agent-001
            agent-name: 日常聊天助手
          module:
            api:
              api-key: sk-xxxx
              base-url: https://example.com
            chat-model:
              model: deepseek-v4-flash
            agent-nodes:
              - key: daily-node
                name: 日常聊天助手
                system-prompt: 负责处理用户日常业务咨询和数据分析的Agent
            agent-runner:
              run-agent-key: daily-node
```

建议将真实 API Key 放在本地私有配置或环境变量中，不要提交到仓库。

## 前端页面说明

- `index.html`：ChatGPT 风格对话页，支持会话历史、本地设置、主题切换、SSE 流式输出。
- `login.html`：账号登录和注册页，支持登录成功后跳转对话页。
- `wechat-login.html`：微信扫码登录页，扫码确认后自动兑换应用登录态并跳转对话页。

前端页面目前是静态资源，无需前端构建流程。

## 开发说明

### `.properties` 中文编码

Spring Boot 默认按 ISO-8859-1 读取 `.properties`。如果配置中需要中文，建议：

- 使用 Unicode 转义。
- 或改用 YAML 配置。
- 或通过 `spring.config.import=classpath:xxx.properties[encoding=utf-8]` 为导入的配置指定编码。

### Knife4j 没有扫描到接口

确认配置中不要给 `.properties` 值加单引号：

```properties
springdoc.group-configs[0].group=default
springdoc.group-configs[0].paths-to-match=/**
springdoc.group-configs[0].packages-to-scan=com.object.ai
```

同时注意上下文路径是 `/api`，访问文档时需要带上 `/api`。

### Redis 配置

如果本地没有 Redis，可以关闭：

```properties
redis.config.enable=false
```

如果启用 Redis，请配置：

```properties
redis.config.enable=true
redis.config.host=localhost
redis.config.port=6379
redis.config.password=
```

### Maven Wrapper

当前仓库未包含 `mvnw`。如果希望其他开发者无需预装 Maven，可以后续补充 Maven Wrapper。

## 常见问题

### 页面接口为什么是 `/api/xxx`？

因为配置了：

```properties
server.servlet.context-path=/api
```

所以静态页面和后端接口都在 `/api` 前缀下。

### 对话接口返回 401 或未登录怎么办？

`AgentController` 使用了 `@SaCheckLogin`，需要先通过账号登录、账号注册或微信扫码登录获取登录态。

### 微信扫码登录需要哪些配置？

需要配置微信公众号的 `app-id`、`secret`、`token`，并将公众号服务器回调地址指向：

```text
http://你的域名/api/wechat/callback
```

本地开发如需调试微信回调，通常需要内网穿透工具。

## 后续可完善

- 增加更完整的用户管理能力，例如封禁、角色授权、资料修改。
- 增加登录页和对话页的登录态校验与退出登录。