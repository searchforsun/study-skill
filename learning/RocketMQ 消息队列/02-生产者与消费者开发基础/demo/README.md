# 阶段 2 示例：生产者（同步/异步/单向）与消费者（PushConsumer/SimpleConsumer）

本目录提供生产者三种发送方式与两种消费者模型的完整可运行示例，覆盖同步发送、异步发送、单向发送、PushConsumer（推模型）与 SimpleConsumer（拉模型+手动ACK）。所有示例共用一个 RocketMQ 5.x Docker Compose 栈。

## 示例总览

| 入口文件 / 目录 | 对应知识点 | 建议顺序 |
|----------------|-----------|----------|
| `docker-compose.yml` | NameServer + Broker + Proxy 基础设施（同阶段 1） | 1（若阶段 1 栈已运行可跳过） |
| `SyncProducerExample.java` | 同步发送、SendReceipt、messageId、Key/Tag 设置 | 3 |
| `AsyncProducerExample.java` | 异步发送、回调线程安全、CountDownLatch 汇聚 | 4 |
| `OnewayProducerExample.java` | 单向发送、无确认、吞吐与可靠性的取舍 | 5 |
| `PushConsumerExample.java` | PushConsumer、MessageListener、ConsumerGroup、Tag 过滤 | 2（先启动，等待消息） |
| `SimpleConsumerExample.java` | SimpleConsumer、receive/ack、invisibleDuration、手动 ACK | 6 |

> 运行顺序说明：消费者必须**先启动并保持运行**，然后再运行生产者发送消息。PushConsumer 和 SimpleConsumer 使用不同的 ConsumerGroup，可同时运行。

## 环境要求

- **操作系统**：64 位；已安装 **Docker** 与 **Docker Compose**。
- **JDK**：**1.8+**；已安装 **Maven 3.6+**。
- **磁盘与内存**：单节点 Broker 约需 2GB 内存；若机器资源紧张，关闭其他容器后再启动。

## 1. 启动 RocketMQ（若尚未启动）

若阶段 1 的容器仍在运行，跳过此步骤。

在**本目录**执行：

```powershell
docker compose up -d
```

检查容器状态：

```powershell
docker ps --filter "name=rmq"
```

停止（学习完成后）：

```powershell
docker compose down
```

## 2. 创建 Topic

```powershell
docker exec rmqbroker sh mqadmin updateTopic -n rmqnamesrv:9876 -t TestTopic -c DefaultCluster -a +message.type=Normal
```

## 3. 编译

进入 `java-client/` 目录：

```powershell
Set-Location java-client
mvn -q -DskipTests compile
```

## 4. 运行示例

### 4.1 PushConsumer（必须先启动）

```powershell
# 终端 A：保持运行，等待消息
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.PushConsumerExample
```

### 4.2 同步发送

```powershell
# 终端 B：发送 5 条消息，终端 A 应看到消费输出
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.SyncProducerExample
```

### 4.3 异步发送

```powershell
# 终端 B：发送 10 条消息，观察回调完成顺序
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.AsyncProducerExample
```

### 4.4 单向发送

```powershell
# 终端 B：消息发出后不等待确认
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.OnewayProducerExample
```

### 4.5 SimpleConsumer

```powershell
# 终端 A（或新终端）：启动 SimpleConsumer（与 PushConsumer 使用不同 ConsumerGroup，互不干扰）
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.SimpleConsumerExample
```

## 5. 集群消费实验（进阶）

**验证同一 ConsumerGroup 内负载均衡**：同时启动两个 PushConsumer 实例（同一 ConsumerGroup），然后运行 SyncProducerExample 发送 20 条消息，观察：

1. 两个消费者各自收到约一半的消息（队列数 ≥ 2 时）。
2. 每条消息只出现在一个消费者的日志中——印证集群消费的"同组内不重复"语义。

做法：在**两个终端**中各自运行：

```powershell
# 终端 A
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.PushConsumerExample "-Drmq.consumer.group=ClusterTestGroup"

# 终端 B
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage02.PushConsumerExample "-Drmq.consumer.group=ClusterTestGroup"
```

> **注意**：Windows PowerShell 中 `-D` 参数需要用引号包裹。

然后在第三个终端发送 20 条消息，观察两个终端的消费分布。

## 学习建议（如何改代码做实验）

1. **SyncProducerExample**
   - 修改 `Topic` 名称为不存在的 Topic，观察同步发送抛出的异常类型与错误信息。
   - 在 `setKeys` 中填入有意义的业务 ID，然后去 Dashboard 按 Key 追踪消息（http://localhost:8080）。
   - 修改 Tag（如 `"PayNotify"`），在 PushConsumerExample 中将过滤表达式改为 `"PayNotify"`，验证 Tag 过滤效果。

2. **AsyncProducerExample**
   - 注释掉 `latch.await()` 那行，观察：程序是否在回调完成前就退出了？
   - 在 `thenAccept` 回调中加 `Thread.sleep(5000)`，观察：异步回调大量堆积对吞吐的影响。
   - 自问：为什么 CountDownLatch 的初始值必须等于总消息数？

3. **PushConsumerExample**
   - 将 `ConsumeResult.SUCCESS` 改为 `ConsumeResult.FAILURE`，观察消息反复重投的日志。
   - 修改 `setConsumerGroup` 使用相同的 Group，同时启动两个实例，发送消息观察负载均衡分配。
   - 把 Tag 过滤表达式从 `"*"` 改为 `"SyncDemo"`，分别运行 SyncProducer 和 AsyncProducer，确认只收到 SyncDemo 的消息。

4. **SimpleConsumerExample**
   - 接收消息后故意**不调用 `ack()`**，观察约 15 秒后同一条消息被再次 `receive()` 拉取。
   - 将 `invisibleDuration` 改为 `Duration.ofSeconds(5)`，验证更短的重投时间窗口。
   - 对比 SimpleConsumer 和 PushConsumer 的代码结构：思考什么场景下你更愿意控制 ACK 时机。

5. **OnewayProducerExample**
   - 发送前故意不创建 Topic，观察：单向发送是否会报错？（不会——这正是它的风险）
   - 思考：若需要"高吞吐 + 可容忍少量丢失"，异步发送是否比单向发送更合适？

## 与 `THEORY.md` 的配合

先阅读理论稿 **「一、生产者三种发送方式」** 与 **「三、PushConsumer 与 SimpleConsumer」**，建立发送选择决策框架和消费者心智模型的对比后运行。跑完示例后回到 **「四、消费者组与负载均衡」** 与 **「五、集群消费与广播消费」**，用集群消费实验的现象印证 Rebalance 与队列粒度分配的讨论。

## 与官方文档的关系

- Compose 结构参考：RocketMQ 文档 **Run RocketMQ with Docker Compose**
- Producer / Consumer 示例参考：官方 Quick Start 中的 **Producer / PushConsumer** 片段
- SimpleConsumer 参考：RocketMQ 官方文档 **消费者分类** 章节

各链接见 `THEORY.md` 文末「推荐阅读」。
