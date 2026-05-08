# 阶段 1 — `demo/` 说明

本目录为 **Maven Java** 工程，演示 Flink DataStream 的最小闭环：**Source → `flatMap` → `keyBy` → 聚合 → Sink**，并用 **`env.execute()`** 触发实际执行。

## 环境要求

| 项目 | 说明 |
|------|------|
| JDK | **17 或更高（必需）**。`pom.xml` 中 Flink **2.0.1** 的依赖为 Java 11+ 字节码；工程编译目标为 **release 17**。若系统默认仍是 JDK 8，`mvn compile` 会报错「class file version 55/61」——请先安装 Temurin / Oracle JDK 等 **JDK 17+**，并将 **`JAVA_HOME`** 与 `PATH` 指到新 JDK，再执行 `java -version` 自查。 |
| Maven | 3.8+ 推荐 |
| 操作系统 | Windows / Linux / macOS 均可；Socket 示例在 Windows 上若无 `nc`，请用 WSL 或下方替代命令 |

## 示例总览

| 顺序 | 入口类 | 知识点 | 依赖 |
|------|--------|--------|------|
| 1（先做） | `BoundedWordCount` | `fromElements`、`flatMap`、`keyBy`、`sum`、`print`、`execute` | 无外部服务 |
| 2（可选） | `SocketWindowWordCount` | `socketTextStream`、处理时间滚动窗口 | 需向 socket 发文本 |

## 构建

在 **`demo/`** 目录执行：

```bash
mvn -q clean compile
```

## 运行

### 1）有界词频（推荐第一步）

```bash
mvn -q exec:java -Dexec.mainClass=com.study.flink.stage01.BoundedWordCount
```

预期：控制台出现若干 `(word, count)` 行；作业结束后进程退出。

**可改动实验**：在 `BoundedWordCount.java` 中修改 `fromElements` 的句子，观察词频变化；尝试调整 `env.setParallelism(...)`，观察 `print` 前缀中的 **subtask 编号**（并行度大于 1 时更明显）。

### 2）Socket + 5 秒滚动窗口（对齐官方教程思路）

终端 A — 监听 **9999** 端口并向作业送数据（任选其一环境）：

```bash
nc -lk 9999
```

若系统无 `nc`，可使用 WSL 中的 `nc`，或 Nmap 自带的 `ncat`（命令形式可能为 `ncat -lk 9999`，以本机帮助为准）。

终端 B — 运行作业：

```bash
mvn -q exec:java -Dexec.mainClass=com.study.flink.stage01.SocketWindowWordCount -Dexec.args="localhost 9999"
```

在终端 A 中多次输入同一单词并回车；**5 秒**处理时间窗口内累计的计数会打印到控制台（详见 `THEORY.md` 中关于窗口的预告）。

## 常见问题

- **下载依赖慢**：配置国内 Maven 镜像或使用单位私服。
- **`socketTextStream` 连接失败**：先确认监听进程已启动且端口未被占用；防火墙拦截时需放行。
- **升级 Flink 版本**：修改 `pom.xml` 中 `flink.version` 后，对照官方 Stable 文档核对 API 是否有变更。

## 与理论文稿的对应关系

- 骨架与惰性执行：见同级目录 **`THEORY.md`**「第一个程序的结构」。
- 运行时组件与部署扩展阅读：见 **`THEORY.md`**「推荐阅读」。
