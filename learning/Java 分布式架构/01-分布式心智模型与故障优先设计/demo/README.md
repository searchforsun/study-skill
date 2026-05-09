# 阶段 1 示例说明：本地并发 vs 分布式语义

本目录示例**不**演示 Redis、注册中心等组件安装；用最小 Java 代码印证 `ROADMAP.md` 阶段 1 中的「本地幻觉」——**在单 JVM 内成立的正确性，不能直接外推为跨进程协作的安全保证**。

## 示例总览

| 入口类 | 对应知识点 | 建议顺序 |
|--------|-------------|----------|
| `RaceConditionDemo` | 竞态、可见性、`synchronized` 与 `AtomicLong` 的**进程内**语义 | 先运行 |
| `CompletableFutureTimeoutDemo` | 超时、链路延迟预算、取消与后台任务的关系；对照「调用方超时 ≠ 服务端停止」 | 后运行 |

## 环境要求

- **JDK**：17 或以上（与 `pom.xml` 中 `maven.compiler.release` 一致）
- **Maven**：3.8+（用于编译与 `exec:java`）
- **操作系统**：Windows / Linux / macOS 均可

## 运行命令

在 **`demo/`** 目录下执行（请把路径换成本机仓库路径）：

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.RaceConditionDemo
```

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.CompletableFutureTimeoutDemo
```

**Windows PowerShell**：`-Dexec.mainClass=...` 可能被拆断，请给参数加引号，例如：

```powershell
mvn -q compile exec:java "-Dexec.mainClass=com.study.distributed.stage01.RaceConditionDemo"
```

也可先 `Set-Location` 到本 `demo` 目录再执行上述命令。

## 学习建议（如何改代码做实验）

1. **RaceConditionDemo**  
   - 先把 `UnsafeCounter` 的 `increment()` 改成「先读后写」分两步并打印中间值，观察差值是否更剧烈（注意日志会拖慢，可把 `rounds` 调小）。  
   - 对比 `SyncCounter`：思考**锁粒度**若过大，对吞吐的影响（本阶段只需建立直觉，优化留到后续）。  
   - 自问：若两个实例各跑一个 JVM，**仅靠** `synchronized` 能否互斥？（答案应是否定，并写出理由。）

2. **CompletableFutureTimeoutDemo**  
   - 调整 `orTimeout` 的毫秒数与 `sleep` 时长，确认：**超时返回**与**后台线程是否结束**不是一回事。  
   - 在场景 C 中增加 `thenApplyAsync` 的级数或单级 `delay`，体会**超时预算被链路吃光**；对照你在服务里配置的「端到端超时」与「单依赖超时」是否分层。  
   - 延伸思考（不写代码也可）：若下游已执行写库，但上游因超时重试，**第二次调用**需要什么机制才能保证安全？

## 与 `THEORY.md` 的配合

先阅读理论稿中 **「Java 并发与本地幻觉」** 与 **「分布式锁」** 两节，再运行示例；运行结果作为「现象锚点」回到文中对照 **故障模型** 与 **幂等/串行化热点** 的讨论。
