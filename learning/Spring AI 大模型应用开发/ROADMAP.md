# Spring AI 大模型应用开发 — 学习路线

**Web 栈约定**：本路线各阶段示例统一基于 **Spring WebFlux**（响应式 Web），使用 Netty 作为运行时；HTTP 接口以返回 **`Mono` / `Flux`**、配合 **SSE（`text/event-stream`）** 暴露流式输出为主，与 Spring AI 的 **`StreamingChatModel` / 流式 ChatClient** 天然契合。

> **生成日期**：2026-05-08  
> **说明**：路线基于当时可查的公开资料与主流实践整理；Spring AI 与 Spring Boot 版本迭代较快，学习过程中请以 [Spring AI Reference](https://docs.spring.io/spring-ai/reference) 与 [Spring Boot 发行说明](https://github.com/spring-projects/spring-boot/wiki) 为准。

## 领域概述

**Spring AI** 是面向 Java / Kotlin 生态的 AI 应用框架，核心目标是：**用统一、可移植的 API 将企业数据、内部 API 与多种大模型（Chat、Embedding、图像、语音等）连接起来**。典型场景包括智能客服、文档问答（RAG）、内部知识库助手、带工具调用的自动化编排等。

本路线假设你已具备 **Java 基础**与 **Spring Boot 入门经验**（IoC、配置、依赖注入）；并建议预先了解 **Project Reactor** 的基本概念（`Mono`、`Flux`、`subscribe` / 非阻塞链）。Web 层使用 **WebFlux**（`@RestController` 返回响应式类型，或 `RouterFunction`），**请勿在同一应用中同时引入 `spring-boot-starter-web`（Spring MVC）**，除非你很清楚混用栈的后果与限制。若尚未掌握 Reactor 入门，可先阅读 [Spring WebFlux 文档「Overview」](https://docs.spring.io/spring-framework/reference/web/webflux.html) 中的响应式编程简介。

## 生态要点（检索摘要）

| 项目 | 说明 |
|------|------|
| **Spring AI** | 官方文档入口：[Spring AI Reference](https://docs.spring.io/spring-ai/reference)；项目页：[spring.io/projects/spring-ai](https://spring.io/projects/spring-ai)。 |
| **Spring Boot** | 官方 Getting Started 写明需配合较新的 Spring Boot 版本（文档持续更新中）；新建项目可用 [Spring Initializr](https://start.spring.io/) 勾选 **Spring WebFlux** 与 Spring AI 相关依赖。 |
| **Spring WebFlux** | 响应式 Web 栈：[WebFlux :: Spring Framework](https://docs.spring.io/spring-framework/reference/web/webflux.html)；与阻塞式 **Spring MVC** 二选一为主流做法，本路线固定采用 WebFlux。 |
| **API 形态** | **ChatClient**、**StreamingChatModel**（`Flux<ChatResponse>` 等流式 API）、**Advisor**、**Vector Store**、**Document / ETL** 等；对外多以 **`Flux` 流 + SSE** 暴露 Token 流。 |
| **版本提示** | Maven Central 上可见 1.0.x 与更新的开发线；具体 BOM 与 artifact 版本以你创建工程当日 **spring-ai BOM** 与官方示例为准。 |

## 学习原则

- 优先阅读 **Spring AI Reference** 与官方示例，再补充社区文章与视频。
- 每一阶段配合本仓库对应阶段的 **`demo/`** 动手运行与改写，再进入下一阶段。
- 调用云端模型时注意 **API Key** 与计费；示例中用环境变量或 `.env`（勿提交密钥）。

## 路线图（自上而下）

```
WebFlux 环境与 Spring Boot · 首个 Chat 调用
              ↓
ChatClient / StreamingChatModel · Flux · SSE · 多模型切换
              ↓
Prompt 工程 · 结构化输出 · 函数/工具调用
              ↓
文档摄入 · 向量存储 · RAG（Advisor）
              ↓
会话记忆 · 可观测性 · 安全与密钥管理
              ↓
WebFlux 综合 API · WebTestClient · 容器与上线要点
```

## 路线总览

| 阶段 | 名称 | 预计周期（参考） | 核心产出 |
|------|------|------------------|----------|
| 1 | WebFlux 环境与 Spring Boot 集成 | 3–5 天 | `spring-boot-starter-webflux` + Spring AI；可运行的最小工程，完成一次 Chat 调用（可从阻塞式 `ChatModel` 入门，再过渡到流式） |
| 2 | ChatClient、Streaming 与 SSE | 4–6 天 | **StreamingChatModel** / ChatClient 流式 API；WebFlux 控制器返回 **`Flux`**，**`produces = MediaType.TEXT_EVENT_STREAM_VALUE`**；切换不同 Provider |
| 3 | Prompt、结构化输出与工具调用 | 5–7 天 | System/User 模板、结构化 Bean 映射、Function Calling / Tools 基础示例 |
| 4 | 文档与向量存储 · RAG | 6–10 天 | Document 读取与切块、VectorStore 写入与检索、QuestionAnswerAdvisor 等 RAG 链路 |
| 5 | 记忆、观测与安全 | 4–6 天 | 会话记忆 Advisor、Micrometer 等观测接入思路、密钥与提示注入防护意识 |
| 6 | 综合实践与部署 | 5–8 天 | WebFlux **RouterFunction** 或 `@RestController` 分层封装；**`WebTestClient`** 与响应式集成测试思路；Docker 与配置外部化 |

## 各阶段详情

### 阶段 1：WebFlux 环境与 Spring Boot 集成

- **目标**：搭建 JDK 17+、Maven 或 Gradle 工程，引入 **`spring-boot-starter-webflux`** 与 Spring AI BOM/依赖，跑通「最小可用」对话调用。
- **核心主题**：Spring Initializr（勾选 **Spring Reactive Web**）、依赖管理、`ChatModel`（或后续替换为 `StreamingChatModel`）与自动配置、`application.yaml` 中的模型与 Base URL。
- **实践要点**：本地或云端任一兼容 Chat Completions 的端点；可先写一个返回 **`Mono<String>`** 的简单 `@RestController` 调用模型，验证链路；排查 401、超时、模型名错误。
- **推荐阅读**：撰写本阶段 `THEORY.md` 时，请检索并核对 [Getting Started :: Spring AI Reference](https://docs.spring.io/spring-ai/reference/getting-started.html)；关键词：`Spring AI getting started`、`spring-ai BOM`、`spring-boot-starter-webflux`。

### 阶段 2：ChatClient、Streaming 与 SSE

- **目标**：使用 **`StreamingChatModel`**（或 ChatClient 的流式路径）输出 **`Flux<ChatResponse>` / `Flux<String>`**，并在 WebFlux 中以 **SSE** 向前端推送 Token 流；理解背压与客户端断开。
- **核心主题**：`ChatClient` builder、stream API；控制器 **`produces = MediaType.TEXT_EVENT_STREAM_VALUE`**；可选 **`RouterFunction`** 风格路由。
- **实践要点**：同一套业务切换不同模型 Bean；用 **curl** 或浏览器 EventSource 验证 SSE；避免在响应式链中阻塞（必要时使用 `subscribeOn` 等策略并参阅官方建议）。
- **推荐阅读**：本阶段 `THEORY.md` 中写入 [Chat Client API :: Spring AI Reference](https://docs.spring.io/spring-ai/reference/api/chatclient.html)、[Chat Model API](https://docs.spring.io/spring-ai/reference/api/chatmodel.html) 中流式相关小节；关键词：`Spring AI StreamingChatModel`、`Spring WebFlux SSE TEXT_EVENT_STREAM`。

### 阶段 3：Prompt、结构化输出与工具调用

- **目标**：掌握 Prompt 模板化、限长与角色设计；使用 Spring AI 的结构化输出能力；理解 Tool/Function 声明与执行流程。
- **核心主题**：`PromptTemplate`、输出解析、Tool 回调与错误处理。
- **实践要点**：定义 1–2 个 Java 函数作为 Tool，由模型决定是否调用；对返回结果做校验。
- **推荐阅读**：以官方 Reference 中 **Structured Output**、**Function Calling / Tools** 章节为准；关键词：`Spring AI structured output`、`Spring AI tools`。

### 阶段 4：文档与向量存储 · RAG

- **目标**：完成「文档 → 切块 → 嵌入 → 向量库 → 检索 → 生成」闭环，并能解释各组件替换点。
- **核心主题**：`Document`、`VectorStore`、`EmbeddingModel`、RAG **Advisor**（如 `QuestionAnswerAdvisor`）、元数据过滤。
- **实践要点**：选用一种向量存储（如内存 Simple、或 PGVector 等，按环境选型）；小语料可重复实验检索质量。
- **推荐阅读**：[Retrieval Augmented Generation :: Spring AI Reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)；关键词：`Spring AI RAG vector store`。

### 阶段 5：会话记忆、可观测与安全

- **目标**：为多轮对话配置记忆策略；了解 Spring AI 与 Micrometer/日志的观测接入；建立 API Key 与提示注入的基本防线。
- **核心主题**：Chat Memory Advisor、观测指标与 Trace、配置分层与密钥管理。
- **实践要点**：限制记忆窗口长度；敏感操作不经由模型直接执行或增加二次确认。
- **推荐阅读**：官方 Reference 中 **Observability**、**Chat Memory** 相关章节；关键词：`Spring AI observability`、`Spring AI chat memory`。

### 阶段 6：综合实践与部署

- **目标**：将前述能力封装为清晰的 **WebFlux API**（响应式控制器或路由），补充测试与容器化，形成可演示的「迷你产品」。
- **核心主题**：分层架构（WebFlux Handler / Controller → Service → Spring AI）、全局异常与超时（如 `WebExceptionHandler`）、**`WebTestClient`** 与 `@SpringBootTest` 下的集成测试、`Dockerfile`、环境变量与配置 Profile。
- **实践要点**：一键启动 README；流式接口的集成测试需理解 **`ExchangeResult`** / 分块响应断言方式；可选 SpringDoc WebFlux 或手写 OpenAPI。
- **推荐阅读**：Spring Framework [WebFlux Testing](https://docs.spring.io/spring-framework/reference/web/webflux-test.html)；Spring Boot [Production-ready Features](https://docs.spring.io/spring-boot/reference/actuator.html)（按需）；关键词：`WebTestClient`、`Spring Boot docker`、`Spring WebFlux integration test`。

## 从基础到实践到进阶

- **基础（阶段 1–2）**：会建 **WebFlux + Spring AI** 工程、会调模型、会用 **Streaming / SSE** 暴露流式对话。  
- **实践（阶段 3–4）**：能控制 Prompt、能用 Tool、能搭一条可演示的 RAG（底层读写仍可与响应式栈共存，注意阻塞型客户端需隔离线程池）。  
- **进阶（阶段 5–6）**：具备记忆与观测意识，并能用 **WebTestClient** 与容器化交付小型响应式服务。

## 路线审阅与确认

- [ ] 我已阅读本路线  
- [ ] 我希望调整：________________（可选）  
- [ ] 确认开始阶段 1 学习：**是** / **否**
