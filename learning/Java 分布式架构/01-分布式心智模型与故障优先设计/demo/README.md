# 阶段一 示例说明：分布式并发与故障模型

本目录示例验证「本地并发直觉在分布式环境下失效」这一核心问题，通过竞态实验、可见性实验和超时模拟，帮助理解故障优先设计的实际含义。

## 示例总览

| 入口文件 / 目录 | 对应知识点 | 建议顺序 |
|----------------|-----------|----------|
| `RaceConditionDemo.java` | 竞态条件（read-modify-write 非原子） | 先运行，多次观察结果差异 |
| `VisibilityDemo.java` | JMM 可见性问题（指令重排/缓存） | 次运行，对照单线程顺序假设 |
| `TimeoutRetryDemo.java` | 超时与重试的边界行为 | 后运行，观察超时后的决策 |
| `DistributedLockDemo.java` | 分布式锁的 TTL/释放校验误区 | 按需，运行前阅读注释 |

## 环境要求

- **JDK**：17+（LTS 版本均可）
- **Maven**：3.8+（用于编译运行）
- **操作系统**：Windows / Linux / macOS 均支持

## 运行命令

### 竞态条件演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.RaceConditionDemo
```

**观察**：多次运行结果不同（多数 < 20000），验证 read-modify-write 的原子性问题。

### 可见性演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.VisibilityDemo
```

**观察**：可能输出 0 或 42，或程序不退出（flag=true 但 value 仍为 0 的情况）。可用 `-X加上-XX:+PrintGC` 观察 GC 影响。

### 超时与重试模拟

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.TimeoutRetryDemo
```

**观察**：三种超时配置的不同行为；指数退避 vs 固定间隔的流量差异（可查看日志时间戳）。

### 分布式锁误区（需自行配置 Redis）

```bash
# 需要本地 Redis 实例运行
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage01.DistributedLockDemo
```

**观察**：
1. 无 TTL 锁在进程 pause 后的行为
2. 释放时未校验导致误删他人锁
3. 主从切换后锁丢失

> 若无 Redis 环境，可阅读源码注释理解误区，实验部分跳过。

## 学习建议（如何改代码做实验）

1. **RaceConditionDemo**
   - 尝试把 `counter++` 换成 `AtomicInteger.incrementAndGet()`，观察结果是否稳定在 20000。
   - 对比 `synchronized` 版本与无锁版本的行为差异。
   - 自问：在分布式环境下，这种竞态会导致什么问题？（提示：库存扣减、余额转移）

2. **VisibilityDemo**
   - 给 `flag` 和 `value` 加上 `volatile`，观察行为变化。
   - 思考：volatile 保证的是可见性还是原子性？
   - 自问：在分布式服务中，哪些变量需要考虑可见性问题？

3. **TimeoutRetryDemo**
   - 调整超时阈值（如从 100ms 改为 10ms），观察误判率。
   - 对比「固定间隔重试」与「指数退避+jitter」在故障恢复时的流量差异。
   - 延伸思考：哪些操作不应该重试？（提示：幂等性）

4. **DistributedLockDemo**
   - 先理解 Redis SETNX 的语义，再思考「为何 TTL 是必须的」。
   - 模拟 Redis 主从切换场景（杀掉主节点），观察锁行为。
   - 对比 ZK/etcd 锁的强一致性语义与 Redis 的弱一致性 trade-off。

## 与 `THEORY.md` 的配合

先阅读理论稿 **「第一章：分布式系统本质：从「确定」到「概率」」** 与 **「第四章：Java 并发最小必要集：本地幻觉的根源」**，再运行示例；运行结果作为「现象锚点」回到文中对照 **「本地幻觉」「竞态条件」「可见性」** 等核心概念的讨论。

---

**补充说明**：本 demo 仅供教学验证，不适合直接用于生产环境。生产级分布式锁请参考 Redisson 或 ZK 官方实现。