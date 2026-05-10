# 阶段 6：云原生交付与架构师职能综合

> 与 [`ROADMAP.md`](../ROADMAP.md) 阶段 6 对齐。动手印证见本目录 [`demo/README.md`](demo/README.md)。**生成说明**：推荐阅读链接基于官方文档入口撰写；组件与默认值持续演进，**请以官方文档与团队规范为准**。

阶段 5 处理了可观测与告警治理——你知道了系统现在怎么样、即将怎么样。本阶段转向**交付侧**：如何把代码安全地送到生产环境，并在交付后保持可控。三个核心问题：**镜像怎么构建才高效**、**容器化 Java 应用有哪些必须调优的参数**、**K8s 部署策略怎么选**，以及贯穿全程的**架构师评审职责**——上线不是点「Deploy」，而是验收、回滚、演练与签字。

## 本阶段知识地图

| 块 | 你要带走的抓手 |
|----|----------------|
| 一 | 镜像分层策略与构建优化——构建时间与镜像大小可直接控制 |
| 二 | JVM 容器参数调优——heap / GC / 线程池与 K8s 资源限制的配合 |
| 三 | K8s 部署策略与探针设计——滚动更新如何配、探针错配怎么杀Pod |
| 四 | 密钥、安全与最小权限——密钥不写进镜像、RBAC 最小角色 |
| 五 | ADR 与架构评审——决策可追溯，结论可辩护 |

**路线要点 ↔ 本文章节**

| `ROADMAP.md` 阶段 6 要点 | 本文展开位置 |
|--------------------------|----------------|
| 镜像分层、JVM 参数、探针、优雅下线 | **一 + 二 + 三** |
| K8s 滚动与会话 | **三** |
| 密钥、最小权限 | **四** |
| SLI/SLO、评审清单、ADR | **五** |
| 软技能：沟通、谈判、推动 | **五** |

---

## 一、镜像分层：构建效率与产出质量

云原生交付的起点是**镜像**。镜像分层决定了构建速度和运行时性能——每一次 `docker build` 的缓存命中与否，直接影响 CI/CD 效率；每一层的大小决定了镜像下载和容器启动时间。

### 1.1 分层原则：善用缓存，缩小变更域

Docker 镜像由一系列只读层（layer）堆叠而成。每一层对应 `Dockerfile` 中的一条指令。构建时，如果某层的内容与上一次相同，Docker 直接复用缓存（cache hit）；如果不同，该层及其后续所有层都需要重新构建（cache miss）。

**分层优化的核心原则**：

1. **不变的指令放前面**：依赖安装、基础配置等不常变化的指令放在 `Dockerfile` 前面，充分利用缓存。
2. **频繁变更的指令放后面**：源代码拷贝、构建产物复制等高频变更的指令放在后面，避免触发大规模重建。
3. **合并 RUN 指令减少层数**：多个 `RUN` 指令如果可以合并，应合并为一条（用 `&&` 连接），减少镜像层数。

**Java 应用的典型 Dockerfile 分层结构**：

```dockerfile
# 阶段 1：构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# 先复制依赖文件（不变的部分）——利用缓存
COPY pom.xml .
RUN mvn dependency:go-offline
# 再复制源代码（高频变更）
COPY src ./src
RUN mvn package -DskipTests

# 阶段 2：运行
FROM eclipse-temurin:17-jre
WORKDIR /app
# 仅复制构建产物（最小运行时）
COPY --from=builder /app/target/myapp.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

上述 Dockerfile 体现了**多阶段构建（Multi-stage Build）**：构建阶段使用完整的 Maven 镜像，运行阶段仅使用 JRE 镜像。运行镜像不包含 Maven 工具链，体积大幅缩小。

### 1.2 多阶段构建的价值

| 维度 | 单阶段构建 | 多阶段构建 |
|------|-----------|-----------|
| 镜像体积 | 包含完整工具链（Java JDK + Maven） | 仅含 JRE + 应用 jar |
| 安全性 | 攻击面大（Maven 可执行任意构建） | 最小运行时，攻击面小 |
| 构建缓存 | 源代码变化触发完整重建 | 依赖层可复用，仅重建应用层 |
| 典型大小 | 800MB ~ 1GB | 200MB ~ 300MB |

### 1.3 .dockerignore：减少构建上下文

构建镜像时，Docker Client 将当前目录（构建上下文）所有文件发送给 Docker Daemon。如果目录包含大量无关文件（`node_modules/`、`target/`、`.git/`），构建速度严重下降且镜像体积膨胀。

在项目根目录创建 `.dockerignore` 文件：

```
.git
.gitignore
*.md
target/
node_modules/
**/*.log
```

### 1.4 镜像优化的两条底线

- **镜像小于 500MB**：过大的镜像拉取慢、启动慢，在 K8s 缩容时影响 Pod 调度效率。
- **不含密钥**：任何密钥（数据库密码、API Token）绝对不能出现在镜像中，即使私有镜像仓库也有泄露风险。密钥通过 K8s Secret 或环境变量注入口令。

---

**本节提要（延伸学习）**

- **核心概念**：Docker 镜像层与缓存机制、多阶段构建（Maven → JRE）、.dockerignore 减少构建上下文、镜像体积与安全性平衡、密钥不入口令
- **拓展提问提示词**

> 主题：Java 应用 Docker 镜像分层策略与构建优化实战。核心概念：Docker layer cache hit/miss 机制、多阶段构建（builder stage vs runtime stage）、.dockerignore 排除规则、Maven dependency:go-offline 离线依赖、镜像体积优化（500MB 以内）、Scratch / Distroless 最小基础镜像。请拓展：Jib 无 Docker 的镜像构建（Google Java 容器化工具）；BuildKit 并行构建与高级缓存；GraalVM native-image 静态编译的镜像体积对比；镜像安全扫描（Trivy / Grype）集成 CI/CD。产出格式：Dockerfile 分层对比表 + 多阶段构建示例 + 构建缓存优化 checklist。

---

## 二、JVM 容器参数：heap、GC 与资源限制的配合

Java 应用容器化的最大陷阱是**JVM 不知道它跑在容器里**。默认情况下，JVM 读取的是宿主机资源信息，而不是 K8s 为容器分配的资源限制。这会导致：容器 CPU 限制 1 核，JVM 以为有 8 核，创建大量 GC 线程或 JIT 编译线程，最终导致容器被 OOM Kill 或被 K8s 限流。

### 2.1 容器感知的关键参数

| 问题 | 默认行为（容器外） | 正确做法 |
|------|-------------------|----------|
| **Heap 大小** | JVM 自动探测宿主机内存，可能超过容器限制 | 显式设置 `-Xms512m -Xmx512m`，或用 `-XX:+UseContainerSupport`（JDK 10+）让 JVM 自动感知 |
| **GC 线程数** | 根据 CPU 核数计算，高并发 GC 可能耗尽容器 CPU | 设置 `-XX:ParallelGCThreads=2 -XX:ConcGCThreads=2` 或用 JDK 17+ 默认容器支持 |
| **Container Support** | JDK 8 及更早版本不感知 cgroup 限制 | 升级至 JDK 11+，或使用 JDK 8u131+（添加了 `-XX:+UseCGroupMemoryLimitForHeap`） |

**推荐的基础 JVM 参数（K8s 容器内）**：

```bash
java -Xms512m -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseContainerSupport \
     -XX:InitialRAMPercentage=70 \
     -XX:MaxRAMPercentage=70 \
     -jar app.jar
```

- `-XX:+UseContainerSupport`：JDK 10+ 让 JVM 自动根据容器 cgroup 限制配置堆、GC 线程。
- `-XX:InitialRAMPercentage=70 -XX:MaxRAMPercentage=70`：将容器可用内存的 70% 用于堆（留 30% 给 Metaspace、Off-heap、缓冲区等）。

### 2.2 典型问题：OOM Kill 与 Pod 重启

在 K8s 中，容器内存使用超过 `limits.memory` 时会被 OOM Kill（OOMKilled）。如果 Pod 不断重启，观察：

1. **JVM Heap 是否足够**：如果 `-Xmx` 小于容器 limits，但系统其他进程（GC 日志、JVM 子进程）占用了额外内存，总内存可能超限。
2. **Off-heap 内存**：Direct ByteBuffer（NIO）、Metaspace、JIT CodeCache 都占用堆外内存，需在 limits 之外考虑。
3. **并非 Heap 泄漏**：有时是正常业务内存峰值（如大促期间的批处理），应与容量规划联动。

**排查命令**：

```bash
# 查看 Pod 重启原因
kubectl describe pod <pod-name> | grep -A5 "Last State"

# 查看容器内存限制
kubectl exec <pod-name> -- cat /sys/fs/cgroup/memory/memory.limit_in_bytes

# 在容器内查看 JVM 探测到的内存
kubectl exec <pod-name> -- java -XX:+PrintFlagsFinal -version | grep MaxHeapSize
```

### 2.3 GC 选型与延迟目标对齐

| GC 收集器 | 适用场景 | 延迟目标 | JVM 参数 |
|-----------|---------|---------|---------|
| **G1** | 默认选择，平衡吞吐与延迟 | P99 < 200ms | `-XX:+UseG1GC -XX:MaxGCPauseMillis=200` |
| **ZGC** | 超大堆（> 100GB）、极低停顿 | P99 < 10ms | `-XX:+UseZGC -XX:+ZGenerational` |
| **Shenandoah** | 与 G1 类似但停顿更短 | P99 < 10ms | `-XX:+UseShenandoahGC` |
| **Parallel** | 高吞吐量批处理 | 追求吞吐，允许较长停顿 | `-XX:+UseParallelGC` |

国内互联网主流选择是 **G1**，因为它对多数场景足够好且参数简单。只有在超大堆或极低延迟需求（如金融交易）时才考虑 ZGC。

---

**本节提要（延伸学习）**

- **核心概念**：JVM 容器感知（UseContainerSupport）、堆大小与容器 limits 的配合、Off-heap 内存（DirectBuffer/Metaspace）、OOM Kill 排查、GC 选型（G1/ZGC/Shenandoah）与延迟目标对齐
- **拓展提问提示词**

> 主题：Java JVM 在 Kubernetes 容器中的参数调优与资源限制配合。核心概念：JVM UseContainerSupport 容器感知机制、-XX:InitialRAMPercentage/MaxRAMPercentage vs -Xms/-Xmx、Off-heap 内存（DirectByteBuffer NIO、Metaspace、JIT CodeCache）与容器 limits 的关系、OOM Kill（OOMKilled）排查与 Pod 重启定位、G1 vs ZGC vs Shenandoah 延迟对比与选型场景。请拓展：JDK 17+ 的 ZGenerational（分代 ZGC）与传统 ZGC 对比；JVM Heap 与容器内存 limits 的 70% 经验公式推导；JDK 8u131 之前的容器支持补丁（UseCGroupMemoryLimitForHeap）；GC 日志与 K8s Pod 日志的关联分析。产出格式：JVM 参数分层配置表（基础/进阶/超大堆）+ OOMKilled 排查流程图 + GC 选型决策矩阵。

---

## 三、K8s 部署策略与探针设计

容器化应用的交付不仅是怎么打镜像，还包括**如何让 K8s 正确地调度、存活、接收流量**。部署策略（Deployment Strategy）和存活探针（Liveness / Readiness Probe）配置错配，是生产环境最常见的事故原因之一：探针太严 → Pod 被误杀；探针太松 → 流量进入未就绪的实例。

### 3.1 部署策略：滚动更新 vs 蓝绿 vs 金丝雀

| 策略 | 原理 | 适用场景 | 风险 |
|------|------|---------|------|
| **滚动更新（RollingUpdate）** | 逐步替换旧版本 Pod，新 Pod 就绪后删除旧 Pod | 无状态服务、多数场景 | 过渡期间新旧版本共存，灰度期间可能有不一致 |
| **蓝绿（Blue-Green）** | 同时运行两套，切换流量指向 | 需要快速回滚、有足够机器 | 资源占用翻倍，切换操作有窗口期 |
| **金丝雀（Canary）** | 只将小比例流量切到新版本，观察后再全量 | 有风险的变更、特性验证 | 流量分配复杂，需要 Ingress 或 Service Mesh 配合 |

**滚动更新配置示例**：

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%       # 最多超出 desired 25% 的 Pod
      maxUnavailable: 25% # 最多不可用 25% 的 Pod
```

- `maxSurge + maxUnavailable` 不能同时为 0。
- 生产环境建议 `maxUnavailable: 0`（保证容量），`maxSurge: 25%` 或更高（加速回滚/上线）。

### 3.2 探针类型与配置原则

K8s 提供三种探针：

| 探针 | 作用 | 失败后果 | 典型场景 |
|------|------|---------|---------|
| **ReadinessProbe** | Pod 是否可以接收流量 | 从 Service Endpoint 摘除，不杀 Pod | 启动预热、依赖检查 |
| **LivenessProbe** | Pod 是否存活 | 杀 Pod 并重启 | 进程僵死、OOM |
| **StartupProbe** | 应用是否启动完成 | 延迟 Liveness 生效 | 启动慢的 Java 应用 |

**Java 应用的探针配置建议**：

```yaml
ports:
- containerPort: 8080
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30    # 等待应用启动
  periodSeconds: 5
  failureThreshold: 3
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60     # 启动探针之后才生效
  periodSeconds:10
  failureThreshold: 3
```

**探针的常见错误**：

- `initialDelaySeconds` 过小：应用还未启动完成，探针就失败了，Pod 被反复重启。
- `/health` 只返回 200 但实际依赖未就绪：探针应检查真实依赖（如 DB 连接池）而非仅检查 HTTP 端口。
- 金丝雀发布时 `maxUnavailable: 50%` 导致容量骤降：对于核心链路，保守的更新策略至关重要。

### 3.3 优雅下线：停止接收新请求，等待在飞请求完成

当 K8s 需要终止 Pod 时（如滚动更新或节点缩容），会先向容器进程发送 `SIGTERM` 信号。应用需要：

1. **停止接收新请求**：从 Service Endpoint 摘除（K8s 自动处理，但应用应尽快关闭监听端口）。
2. **等待在飞请求完成**：设置合理的 `terminationGracePeriodSeconds`（默认 30 秒），期间继续处理已有请求。
3. **关闭数据库连接池**：应用退出前应等待连接池清空，避免请求中断导致事务失败。

```yaml
spec:
  terminationGracePeriodSeconds: 60
```

Java 应用应在 Shutdown Hook 中关闭 Web 服务器（停止接收新请求）并等待线程池清空：

```java
@PreDestroy
public void onShutdown() {
    // 停止接收新请求
    ((TomcatWebServer) tomcat).stop();
    // 等待现有请求完成（最多 30 秒）
    executor.shutdown();
    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
}
```

---

**本节提要（延伸学习）**

- **核心概念**：滚动更新（RollingUpdate）参数配置、金丝雀（Canary）与蓝绿（Blue-Green）适用场景、ReadinessProbe vs LivenessProbe vs StartupProbe 区分、探针失败导致 Pod 重启的排查、优雅下线 SIGTERM 与 Shutdown Hook、terminationGracePeriodSeconds 配置
- **拓展提问提示词**

> 主题：Kubernetes 部署策略（RollingUpdate/Canary/Blue-Green）与探针设计实战。核心概念：maxSurge/maxUnavailable 滚动更新参数、滚动更新期间新旧版本共存的流量管理、ReadinessProbe 使 Pod 从 Service 摘除、LivenessProbe 触发 Pod 重启的条件、StartupProbe 延迟 Liveness 探针生效的机制、优雅下机的 SIGTERM/SIGKILL 与 Shutdown Hook、terminationGracePeriodSeconds 与在飞请求完成的关系。请拓展：Istio/Envoy 的金丝雀流量分割（权重路由 vs Header 路由）；滚动更新期间的 trace 连续性（traceId 在新旧 Pod 间的传播）；Spring Boot Actuator / Micrometer 与 K8s 探针集成的健康检查端点；滚动更新时 Prometheus 指标的空档期处理。产出格式：三种部署策略对比表 + K8s Deployment YAML 示例 + 探针配置决策树 + 优雅下线代码示例。

---

## 四、密钥、安全与最小权限

云原生交付不仅是「能不能跑起来」，更是「跑起来是否安全」。两个最常见的问题是：**密钥明文写在镜像或配置里**，以及**RBAC 权限过大**。这两个问题在国内互联网生产环境中都曾导致过严重的安全事故。

### 4.1 密钥管理：绝对不入口令

**绝对禁止**：

- 在 Dockerfile / application.yml 中硬编码密码
- 在 Git 仓库中提交包含密钥的配置文件
- 在镜像中存储密钥（即使私有仓库也有泄露风险）

**正确做法：K8s Secret 或外部密钥管理服务**

| 方案 | 适用场景 | 说明 |
|------|---------|------|
| **K8s Secret（Base64）** | 简单场景，密钥不频繁轮换 | 数据未加密（Base64 只是编码），需配合 RBAC 限制谁可以读 |
| **K8s Secret + Vault** | 密钥需加密存储与自动轮换 | HashiCorp Vault 与 K8s 集成，密钥动态生成 |
| **云厂商密钥管理（KMS）** | 国内云环境（如阿里云 ACK / 腾讯云 TKE） | 使用云 KMS 服务，Pod 通过 ServiceAccount 映射权限 |

**K8s Secret 使用示例**：

```yaml
# 创建 Secret（Base64 编码）
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
type: Opaque
data:
  # echo -n "password" | base64
  password: cGFzc3dvcmQ=
---
# Pod 引用 Secret
env:
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: password
```

### 4.2 RBAC 最小权限原则

K8s RBAC（Role-Based Access Control）决定了谁可以做什么操作。默认的 `cluster-admin` 权限过于宽泛，生产环境应遵循**最小权限原则**。

**常见的 RBAC 角色错误**：

- 应用 Pod 被授予 `cluster-admin`（集群最高权限），一旦 Pod 被攻陷，攻击者可以控制整个集群。
- `ServiceAccount` 绑定了过大的 `ClusterRole`，导致权限扩散。

**最小权限示例（应用 Pod 只读自身 Pod 信息）**：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: app-pod-reader
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: app-pod-reader-binding
subjects:
- kind: ServiceAccount
  name: app-sa
  namespace: production
roleRef:
  kind: Role
  name: app-pod-reader
```

### 4.3 镜像安全扫描

每次 CI/CD 构建镜像后，应自动执行安全扫描。常用工具：

| 工具 | 说明 |
|------|------|
| **Trivy** | 轻量级镜像扫描，兼容 K8s CI/CD |
| **Grype** | Anchore 出品的扫描工具 |
| **Clair** | CoreOS 镜像扫描（较重） |

扫描发现问题（如 CVE 高危漏洞）应阻断发布，而非上生产再发现。

---

**本节提要（延伸学习）**

- **核心概念**：K8s Secret 与 Base64 编码风险、KMS 密钥管理与动态轮换、RBAC 最小权限原则与 ClusterRole vs Role、镜像安全扫描（Trivy/Grype）与 CVE 阻断、ServiceAccount 权限隔离、Pod Security Standards
- **拓展提问提示词**

> 主题：云原生安全：K8s 密钥管理、RBAC 最小权限与镜像安全扫描。核心概念：K8s Secret Base64 编码非加密的风险、Secret 与 ConfigMap 的区别与应用场景、HashiCorp Vault 与 K8s 集成（动态密钥）、RBAC Role/ClusterRole/RoleBinding/ClusterRoleBinding 层级、ServiceAccount 最小权限原则（应用 Pod 不应使用 cluster-admin）、Trivy/Grype 镜像扫描集成 CI/CD、Pod Security Standards（Baseline/Restricted 级别）。请拓展：阿里云 ACK / 腾讯云 TKE 的 KMS 集成；Kyverno / OPA 策略引擎在 Pod 安全上下文的应用；镜像签名与cosign 签名验证；网络策略（NetworkPolicy）在命名空间级别的隔离。产出格式：密钥管理方案对比表 + RBAC 最小权限 YAML 示例 + 安全扫描 CI/CD 集成 pipeline 示例。

---

## 五、ADR 与架构评审：决策可追溯与结论可辩护

架构师的核心产出不是代码，而是**决策**。每一个技术决策——选型 A 而非 B、在某处做 Trade-off、拒绝某个方案——都应有记录。ADR（Architecture Decision Record，架构决策记录）是这个记录的载体。

### 5.1 ADR 的结构与价值

**ADR 是「某个时间点、某个约束下、做出某个决策」的可追溯文档。** 它不是设计文档，而是决策日志。三年后回看 ADR，能理解当时为什么这么选，而不是盲目继承一段无人能解释的代码。

**ADR 标准结构**：

```
# ADR-001: 使用 G1 作为默认 GC 收集器

## 状态
Accepted（已接受）

## 背景
订单服务 P99 延迟在高并发下超过 500ms，GC 停顿是主要贡献者。

## 决策
采用 G1 GC 收集器，设置 -XX:MaxGCPauseMillis=200。

## 备选方案
1. ZGC：延迟更低，但 JDK 11+ 且 G1 在多数场景足够。
2. Parallel GC：吞吐更高，但停顿时间不可控。

## 后果
正面：P99 延迟从 800ms 降至 300ms。
负面：G1 在极端大堆（> 100GB）时表现下降。

## 回滚条件
如果 P99 延迟无法降到 500ms 以内，回滚至 Parallel GC。
```

### 5.2 架构评审清单：上线前的最后一道门

架构评审不是走过场，而是**在代码提交前发现问题的最后机会**。典型评审维度：

| 维度 | 评审要点 |
|------|---------|
| **故障模型** | 最坏情况会影响哪些用户？影响面多大？有没有熔断/降级？ |
| **数据一致性** | 跨服务数据一致性如何保证？事务边界与领域边界是否对齐？ |
| **可观测** | 关键路径是否有 trace 埋点？告警阈值是否与 SLO 对齐？ |
| **安全** | 密钥是否上 KMS？RBAC 是否最小权限？镜像是否有 CVE？ |
| **容量** | 有无压测数据？扩容策略是什么？是否依赖单点？ |
| **交付** | 回滚方案是什么？金丝雀/灰度策略是什么？是否需要停机窗口？ |

### 5.3 架构师的软技能：沟通、谈判、推动

架构师的工作有 50% 在技术之外的协作与沟通：

- **沟通**：把复杂的技术决策用非技术人员能理解的语言解释清楚。
- **谈判**：在资源约束（时间、人力、成本）下找到技术方案与业务目标的平衡。
- **推动**：技术方案确定后，推动各团队落地执行，克服组织惯性。

架构评审中，架构师需要主持讨论、收集各方意见、引导决策，而不是单向宣布结论。评审的结论应该经过充分讨论，让各方对决策背后的 Trade-off 有共识。

---

**本节提要（延伸学习）**

- **核心概念**：ADR（架构决策记录）标准结构（状态/背景/决策/备选方案/后果/回滚条件）、架构评审清单维度（故障模型/一致性/可观测/安全/容量/交付）、架构师软技能（沟通/谈判/推动）、ADR 与代码的关联（ADR 编号出现在代码注释或 Commit Message 中）
- **拓展提问提示词**

> 主题：架构决策记录（ADR）规范与架构评审实战。核心概念：ADR 标准格式（背景/决策/备选方案/后果/回滚条件）、ADR 在团队中的可见性（代码注释关联、ADR 仓库索引）、架构评审清单（故障模型/一致性/可观测/安全/容量/交付）、架构师沟通谈判推动的实践方法、ADR 生命周期（Proposed/Accepted/Deprecated）与变更记录。请拓展：ADR 工具（Log4j ADR、ADR-Tools 自动生成）、架构评审会议的组织（时长/参与者/输出物）、SLO/SLI 在架构评审中的角色（技术承诺与业务约束对齐）、Google ARC 案例（架构评审委员会的有效实践）。产出格式：ADR 模板 + 架构评审 SOP + 评审会议议程模板。

---

## 推荐阅读

> 说明在前、链接行仅 URL（复制时不会夹带额外字符）。

- **Kubernetes 官方文档 — Workloads: Deployment**
  - 关联主题：滚动更新策略、maxSurge/maxUnavailable 配置、探针配置示例。
  - https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
  - 检索：`Kubernetes Deployment rollingUpdate maxSurge maxUnavailable`

- **Eclipse Temurin Docker 镜像官方文档**
  - 关联主题：多阶段构建的 JRE 基础镜像、容器内 JVM 参数最佳实践。
  - https://hub.docker.com/tags/eclipse-temurin
  - 检索：`Eclipse Temurin Java container JVM parameters`

- **Google Cloud — Kubernetes Engine: Best Practices**
  - 关联主题：探针配置、密钥管理、RBAC 最小权限、镜像安全。
  - https://cloud.google.com/kubernetes-engine/docs/best-practices?hl=zh-cn
  - 检索：`GKE best practices security RBAC probe`

- **Kubernetes 官方文档 — Authentication Authorization RBAC**
  - 关联主题：RBAC Role/ClusterRole/RoleBinding 配置，最小权限原则。
  - https://kubernetes.io/docs/reference/access-authn-authz/rbac/
  - 检索：`Kubernetes RBAC role clusterrole binding example`

- **OpenTelemetry — K8s Deployment Best Practices**
  - 关联主题：K8s 部署中的可观测性集成、探针与指标暴露。
  - https://opentelemetry.io/docs/kubernetes/
  - 检索：`OpenTelemetry Kubernetes deployment observability`

- **ADR 官方文档 — Why and How**
  - 关联主题：ADR 标准格式、决策记录的价值与维护方法。
  - https://adr.github.io/
  - 检索：`Architecture Decision Record ADR format why how`