# 阶段 5 示例：可观测、容量与稳定性治理

本目录提供**可观测三根、黄金信号与 RED/USE 框架、SLO 与错误预算、容量规划、告警治理**的纯 Java 演示——不依赖 SkyWalking / Prometheus / Grafana，聚焦**概念逻辑与决策框架**。

## 示例总览

| 入口文件 | 对应知识点 | 建议顺序 |
|---------|-----------|----------|
| `GoldenSignalsDemo.java` | 可观测三根（Trace/指标/日志）、黄金信号（Latency/Traffic/Errors/Saturation）、RED/USE 框架、P99 计算 | 1 |
| `SLODemo.java` | SLO 制定流程、错误预算计算与消耗速率、容量规划（线程数/响应时间 → QPS）、压测场景与扩缩容决策、降级策略配置 | 2 |
| `AlertDemo.java` | 固定阈值 vs 动态基线、告警聚合与同源抑制、速率驱动告警级别（P1/P2/P3）、维护窗口、告警规则设计模板 | 3 |

## 环境要求

- **JDK**：**17+**
- **Maven**：**3.6+**
- 纯 Java，无外部依赖

## 运行命令

### 编译

```powershell
Set-Location "d:\MyWorkStation\Java\program\study-skill\learning\Java 分布式架构\05-可观测、容量与稳定性治理\demo"
mvn -q -DskipTests compile
```

### 运行各演示

```powershell
# 演示 1：可观测三根与黄金信号（RED/USE 框架）
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage05.GoldenSignalsDemo

# 演示 2：SLO、错误预算与容量规划
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage05.SLODemo

# 演示 3：告警治理 — 降噪与可信
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage05.AlertDemo
```

## 学习建议（如何改代码做实验）

### 1. GoldenSignalsDemo
- **改模拟的负载曲线**：把 `second > 20` 后的 CPU/连接数增长调大/调小，观察 P99 变化。
- **改失败率**：把 `random.nextDouble() > 0.05` 改为 `0.10` 或 `0.02`，观察错误率对告警的影响。
- **自问**：为什么 P99 不是平均值 × 1.5？什么情况下 P99 会远超平均值？

### 2. SLODemo
- **改 SLO 配置**：把 `availabilityTarget` 从 99.5 改为 99.9，观察错误预算如何变化。
- **改容量规划参数**：把 `threadPoolSize` 从 200 改为 500，观察理论 QPS 如何变化。
- **改压测场景的负载因子**：`loadFactor = qps / 理论 QPS` 时，观察延迟的非线性增长。
- **自问**：如果错误预算消耗速率是"1 天消耗 1 周预算"，应该是什么告警级别？

### 3. AlertDemo
- **改动态基线倍数**：把 `baselineMultiplier` 从 1.2 改为 1.5，观察告警触发条件的变化。
- **改错误注入速率**：在 `RateBasedAlerting` 场景中，把 `30 次/分钟` 改为 `100 次/分钟`，观察 P1 触发的速度。
- **自问**：为什么凌晨低流量时固定阈值会误报？动态基线如何解决这个问题？

## 与 `THEORY.md` 的配合

先阅读理论稿对应章节再运行对应 demo：
- **「一、可观测三根」** + **「二、黄金信号与 RED/USE」** → `GoldenSignalsDemo`
- **「三、SLO 与错误预算」** + **「四、容量规划」** → `SLODemo`
- **「五、告警治理」** → `AlertDemo`

## 与真实中间件的关系

| 概念 | 真实工具 |
|------|---------|
| 链路追踪 | SkyWalking（Java Agent）、Jaeger（OpenTelemetry）、Zipkin |
| 指标存储/查询 | Prometheus TSDB + PromQL、Grafana |
| 日志聚合 | ELK（Elasticsearch/Logstash/Kibana）、Loki + Grafana |
| 告警路由/抑制 | Prometheus AlertManager（inhibit_rules）、PagerDuty |
| 全链路可观测 | OpenTelemetry SDK + Collector + OTLP 协议 |

本 demo 聚焦"为什么会这样"和"如何做决策"，具体的配置方法请查阅各工具官方文档。

## 核心结论速查

| Demo | 核心结论 |
|------|---------|
| `GoldenSignalsDemo` | 盯住 4~6 个核心指标；RED 用于无状态服务，USE 用于基础设施；P99 是 SLA 承诺，不是平均值 |
| `SLODemo` | 错误预算 = (1 - SLO) × 请求量；消耗速率驱动告警级别；延迟非线性增长（排队效应） |
| `AlertDemo` | 固定阈值不适应业务周期；聚合抑制减少噪声；速率驱动让告警与业务影响挂钩 |