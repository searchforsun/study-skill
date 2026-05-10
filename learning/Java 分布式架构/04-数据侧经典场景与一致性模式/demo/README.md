# 阶段 4 示例：数据侧经典场景与一致性模式

本目录提供**缓存防御、读一致性分级、Saga/Outbox 事务模式、分键决策**四个维度的纯 Java 演示——不依赖 Redis / MySQL / MQ，聚焦**模式逻辑与 trade-off 对比**。

## 示例总览

| 入口文件 | 对应知识点 | 建议顺序 |
|---------|-----------|----------|
| `CacheDefenseDemo.java` | 穿透（布隆+空值）、击穿（互斥锁双重检查）、雪崩（随机TTL）、Cache-aside先库后删 | 1 |
| `SelectiveReadMasterDemo.java` | 主从延迟、读一致性三级、selective-read-master（近期写入标记+TTL） | 2 |
| `SagaOutboxDemo.java` | Saga编排式（正向+逆序补偿）、Outbox本地消息表（同DB事务+轮询投递） | 3 |
| `ShardingKeyDemo.java` | 分键选择、数据倾斜、全分片扫描代价、决策前置四问 | 4 |

## 环境要求

- **JDK**：**17+**（与 Stage 3 一致）
- **Maven**：**3.6+**
- 纯 Java，无外部依赖

## 运行命令

### 编译

```powershell
Set-Location "d:\MyWorkStation\Java\program\study-skill\learning\Java 分布式架构\04-数据侧经典场景与一致性模式\demo"
mvn -q -DskipTests compile
```

### 运行各演示

```powershell
# 演示 1：缓存防御（穿透/击穿/雪崩/Cache-aside）
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage04.CacheDefenseDemo

# 演示 2：读写分离与选择性读主
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage04.SelectiveReadMasterDemo

# 演示 3：Saga 编排式事务 + Outbox 本地消息表
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage04.SagaOutboxDemo

# 演示 4：分库分表 — 分键选择与数据倾斜
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage04.ShardingKeyDemo
```

## 学习建议（如何改代码做实验）

### 1. CacheDefenseDemo
- **改布隆过滤器参数**：将 `falsePositiveRate` 从 0.01 改为 0.10，观察假阳性概率上升后过滤器大小变化。
- **改击穿演示的并发数**：把 10 个线程改为 100 个，观察无锁 vs 有锁时查库次数的差异（应始终为 1）。
- **改雪崩的 jitterRatio**：从 0.20 调到 0.05，观察离散度的减小。
- **自问**：为什么双重检查在 `synchronized` 块内还要再查一次缓存？

### 2. SelectiveReadMasterDemo
- **改 replicationLagMs**：从 200ms 改为 500ms，观察 TTL 跟随调整的必要性。
- **思考**：TTL 设太长（如 30s）的代价是什么？设太短（如 50ms）的风险是什么？
- **自问**：如果 Redis（存 recentlyWritten 标记）宕机了，这个方案应该降级为"全部读主"还是"全部读从"？

### 3. SagaOutboxDemo
- **改库存步骤**：注释/取消注释 `throw new RuntimeException("库存不足")` 切换成功/失败路径，观察补偿执行顺序。
- **改 Outbox 轮询间隔**：把 `pollingSend` 中的逻辑想象成定时任务，思考轮询间隔 1s vs 5s 对一致性的影响。
- **自问**：如果 Outbox 定时任务在投递到 MQ 成功、但更新 status=SENT 之前崩溃了，会发生什么？

### 4. ShardingKeyDemo
- **改状态分布**：调整各状态的百分比，观察倾斜是否进一步恶化。
- **思考**：如果要用 `userId` 作为分键，但存在"查某商户所有订单"的需求（不带 userId），该如何补充查询能力？

## 与 `THEORY.md` 的配合

先阅读理论稿对应章节再运行对应 demo：
- **「一、缓存模式」** → `CacheDefenseDemo`
- **「二、读写分离」** → `SelectiveReadMasterDemo`
- **「三、分布式事务」** 与 **「四、消息语义」** → `SagaOutboxDemo`
- **「五、分库分表」** → `ShardingKeyDemo`

## 与真实中间件的关系

- 布隆过滤器：Redisson `RBloomFilter` / Guava `BloomFilter`
- 分布式锁：Redisson `RLock` + watchdog / `tryLock` + 身份校验
- Saga：Seata Saga 状态机 DSL
- Outbox：自建定时轮询 / Debezium CDC + Kafka
- 分库分表：ShardingSphere-JDBC / Mycat

本 demo 聚焦"为什么会这样"和"为什么这样选"，具体的 API 使用请对应查阅各中间件官方文档。
