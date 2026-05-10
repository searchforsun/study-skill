# 阶段 6 示例：云原生交付与架构师职能综合

本目录提供**镜像分层策略、JVM 容器参数配置、K8s 部署策略与探针、密钥安全与 RBAC、ADR 决策记录与架构评审**的纯 Java 演示——不依赖真实的 K8s/Docker 环境，聚焦**概念逻辑与决策框架**。

## 示例总览

| 入口文件 | 对应知识点 | 建议顺序 |
|---------|-----------|----------|
| `CloudNativeDeliveryDemo.java` | 镜像分层与多阶段构建、JVM 容器感知参数、ADR 标准结构与架构评审清单、架构师软技能 | 1（合并三个模块） |

## 环境要求

- **JDK**：**17+**
- **Maven**：**3.6+**
- 纯 Java，无外部依赖

## 运行命令

### 编译

```powershell
Set-Location "d:\MyWorkStation\Java\program\study-skill\learning\Java 分布式架构\06-云原生交付与架构师职能综合\demo"
mvn -q -DskipTests compile
```

### 运行演示

```powershell
mvn -q exec:java -Dexec.mainClass=com.study.distributed.stage06.CloudNativeDeliveryDemo
```

## 学习建议（如何改代码做实验）

### CloudNativeDeliveryDemo

**模块一：镜像分层**
- 调整 `Layer` 类的 `changed` 概率，观察缓存命中率变化。
- 对比单阶段与多阶段构建的镜像大小，验证多阶段的价值。
- 自问：为什么 Dockerfile 中 `COPY pom.xml` 要放在 `COPY src` 之前？

**模块二：JVM 容器参数**
- 调整 `containerMemoryLimit` 从 512MB 改为 256MB，观察 heap 比例是否仍合理。
- 调整 `MaxRAMPercentage` 从 70% 改为 90%，观察超限风险。
- 自问：为什么 Heap 不建议设置为容器 limits 的 100%？Off-heap 包含哪些组件？

**模块三：ADR 与架构评审**
- 参考 ADR 示例格式，为自己项目中的某个技术决策写一份 ADR。
- 检查架构评审清单的 6 个维度，对照自己当前项目的情况。
- 自问：密钥目前是否在 ConfigMap 或代码中？需要迁移到 KMS 吗？

## 与 `THEORY.md` 的配合

先阅读理论稿对应章节再运行对应 demo：

| 理论章节 | Demo 模块 |
|---------|----------|
| **「一、镜像分层」** | 模块一：镜像分层与多阶段构建 |
| **「二、JVM 容器参数」** | 模块二：JVM 容器感知与参数配置 |
| **「三、K8s 部署策略」** | 模块三中探针与优雅下线相关部分 |
| **「四、密钥安全」** | ADR 示例中的安全评审维度 |
| **「五、ADR 与架构评审」** | 模块三：ADR 标准结构与评审清单 |

## 与真实中间件的关系

| 概念 | 真实工具 |
|------|---------|
| 容器镜像构建 | Docker/BuildKit、Buildpacks、Jib |
| Java 运行时镜像 | Eclipse Temurin（HotSpot）、Azul Zulu、Amazon Corretto |
| K8s 部署策略 | RollingUpdate、Canary（Istio/Argo Rollouts）、Blue-Green |
| 探针与健康检查 | K8s LivenessProbe/ReadinessProbe、Spring Boot Actuator |
| 密钥管理 | HashiCorp Vault、阿里云 KMS、腾讯云 CKMS |
| RBAC 最小权限 | K8s RBAC（Role/ClusterRole）、OPA/Kyverno 策略引擎 |
| 镜像安全扫描 | Trivy、Grype、Clair |
| ADR 管理 | ADR-Tools、Log4j ADR、MKdocs 插件 |

本 demo 聚焦"为什么会这样"和"如何做决策"，具体的 YAML 配置方法请查阅各工具官方文档。

## 核心结论速查

| 模块 | 核心结论 |
|------|---------|
| 镜像分层 | 多阶段构建：镜像从 ~900MB 降至 ~330MB；不变指令放前面利用缓存 |
| JVM 参数 | JDK 10+ 用 `-XX:+UseContainerSupport`；Heap 不超过容器 limits 的 70% |
| K8s 部署 | 滚动更新 `maxUnavailable=0`；ReadinessProbe 先于 LivenessProbe |
| 密钥安全 | 密钥必须上 KMS，禁止明文在 ConfigMap 或镜像中 |
| ADR 与评审 | ADR 记录"为什么选"，不是"做什么"；评审清单 6 个维度必查 |