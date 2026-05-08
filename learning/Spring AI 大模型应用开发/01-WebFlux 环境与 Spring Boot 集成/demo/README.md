# 阶段 1 示例：WebFlux + Spring AI（同步 ChatModel）

本目录为「WebFlux 环境与 Spring Boot 集成」阶段的**可运行骨架**：使用 **`spring-boot-starter-webflux`** 与 **`spring-ai-starter-model-openai`**，通过 **`ChatModel`** 发起一次同步对话，并在控制器中用 **`Mono` + `boundedElastic`** 卸载阻塞调用。

## 环境要求

| 项目 | 说明 |
|------|------|
| **JDK** | **17 或更高**。Spring Boot 3.x 与 Spring AI 当前主线均要求至少 Java 17；若 `mvn compile` 报错「class file version 61」与当前 JDK 不匹配，请安装 JDK 17+ 并设置 `JAVA_HOME`。 |
| **Maven** | 3.8+（或使用 IDE 自带 Maven） |
| **网络** | 可访问 OpenAI API（若使用默认 `base-url`）；企业网络需配置代理时请参考 Spring Boot / JVM 代理文档 |
| **密钥** | 有效的 **OpenAI API Key**（通过环境变量注入，勿写入仓库） |

## 文件说明与学习顺序

| 顺序 | 文件 / 目录 | 作用 |
|------|----------------|------|
| 1 | `pom.xml` | Spring Boot 父 POM、`spring-ai-bom` **1.0.6**、WebFlux 与 OpenAI Starter |
| 2 | `src/main/resources/application.yml` | 应用名、端口、`spring.ai.openai` 与默认模型名 |
| 3 | `SpringAiStage01Application.java` | 启动类 |
| 4 | `web/ChatController.java` | **`Mono.fromCallable` + `subscribeOn(boundedElastic)`** 调用 `ChatModel` |
| 5 | `.env.example` | 环境变量示例（复制思路；Windows 可用「系统环境变量」或 PowerShell `$env:OPENAI_API_KEY`） |

建议先通读本阶段上级目录的 **`THEORY.md`**，再对照本表打开源码。

## 配置 API Key

**不要**把真实密钥写入 `application.yml` 或提交到 Git。

**PowerShell（当前会话）**：

```powershell
$env:OPENAI_API_KEY = "sk-你的密钥"
```

**Linux / macOS**：

```bash
export OPENAI_API_KEY="sk-你的密钥"
```

`application.yml` 中已使用 `${OPENAI_API_KEY}`，未设置时应用**无法**通过 OpenAI 自动配置正常调用模型（启动行为以当前 Spring AI 版本为准，常见为校验失败或调用报错）。

## 构建与运行

在 **`demo/`** 目录执行：

```bash
mvn -DskipTests compile
mvn spring-boot:run
```

启动成功后默认端口 **`8080`**。

## 验证接口

浏览器或 HTTP 客户端访问：

```text
http://localhost:8080/api/chat
```

带自定义问题：

```text
http://localhost:8080/api/chat?message=用一句话说明WebFlux与Spring MVC的主要差异。
```

**curl 示例**：

```bash
curl -s "http://localhost:8080/api/chat?message=Hello"
```

返回Body为**纯文本**（模型回复）。

## 建议动手实验

1. 修改 `application.yml` 中的 **`spring.ai.openai.chat.options.model`**，观察不同模型在延迟与回答风格上的差异（须账号支持该模型）。
2. 暂时去掉 `subscribeOn(Schedulers.boundedElastic())`，在高并发压测下体会对延迟的影响（仅作实验，正式代码请保留卸载）。
3. 故意填错 `OPENAI_API_KEY`，观察日志与 HTTP 状态，练习对照 **`THEORY.md`** 中的排查表。

## 版本说明

- **Spring Boot**：`3.4.5`（见 `pom.xml` 的 `parent`）
- **spring-ai-bom**：`1.0.6`

升级时请同时查阅 [Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html) 中与 **Spring Boot**、**artifact 命名**相关的说明，避免仅改一个版本号导致不兼容。
