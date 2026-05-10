# 阶段三 示例说明：韧性工程与交互故障防御

本目录示例通过超时模拟、熔断器行为、舱壁隔离等实验，验证「假设故障 → 防御 → 降级」的韧性工程思路。

## 示例总览

| 入口文件 / 目录 | 对应知识点 | 建议顺序 |
|----------------|-----------|----------|
| `TimeoutChainDemo.java` | 超时链设计、分层超时配置 | 先运行，理解超时层级 |
| `RetryDemo.java` | 重试三原则、指数退避+jitter、重试风暴 | 次之，观察退避效果 |
| `CircuitBreakerDemo.java` | 熔断器三状态转换 | 后运行，理解熔断触发与恢复 |
| `BulkheadDemo.java` | 线程池隔离、舱壁模式 | 按需，理解资源隔离 |
| `RateLimitDemo.java` | 限流算法（令牌桶/滑动窗口） | 按需，观察限流效果 |

## 环境要求

- **JDK**：17+
- **Maven**：3.8+

## 运行命令

### 超时链演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage03.TimeoutChainDemo
```

**观察**：不同超时配置下的调用结果；过短超时导致的误判率。

### 重试演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage03.RetryDemo
```

**观察**：固定间隔重试 vs 指数退避+jitter 的流量差异；幂等性判断。

### 熔断器演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage03.CircuitBreakerDemo
```

**观察**：熔断器从 Closed→Open→Half-Open→Closed 的状态转换；降级行为。

### 舱壁隔离演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage03.BulkheadDemo
```

**观察**：独立线程池 vs 共享线程池的行为差异；资源耗尽时的隔离效果。

### 限流演示

```bash
mvn -q compile exec:java -Dexec.mainClass=com.study.distributed.stage03.RateLimitDemo
```

**观察**：令牌桶限流 vs 滑动窗口限流的行为差异；突发流量的处理。

## 学习建议（如何改代码做实验）

1. **TimeoutChainDemo**
   - 调整每跳超时配置，观察误判率与雪崩风险的 trade-off。
   - 对比「全局统一超时」与「分层超时」的效果差异。
   - 思考：如何根据下游 SLO 动态调整超时？

2. **RetryDemo**
   - 把指数退避+jitter 改为固定间隔，观察流量尖峰。
   - 模拟幂等操作（如读请求）vs 非幂等操作（如支付）的重试风险。
   - 延伸思考：哪些场景下重试是危险的？

3. **CircuitBreakerDemo**
   - 调整失败率阈值（如从 50% 改为 20%），观察熔断敏感度变化。
   - 模拟熔断恢复时的「半开」状态，理解为何逐步放量。
   - 对比熔断 vs 直接超时的行为差异。

4. **BulkheadDemo**
   - 增加并发请求数，观察共享线程池 vs 独立线程池的响应时间差异。
   - 思考：哪些服务适合线程池隔离？隔离后如何监控？

5. **RateLimitDemo**
   - 对比令牌桶与滑动窗口的突发流量处理能力。
   - 模拟超出限流阈值的请求行为（拒绝 vs 排队）。
   - 延伸思考：入口限流与服务间限流的区别与配合。

## 与 `THEORY.md` 的配合

先阅读理论稿 **「第二章：超时链设计」** 与 **「第三章：重试策略」**，再运行超时与重试示例；然后阅读 **「第四章：熔断器」** 与 **「第五章：舱壁模式」**，运行熔断与舱壁示例；最后阅读 **「第六章：限流与降级」**，运行限流示例。