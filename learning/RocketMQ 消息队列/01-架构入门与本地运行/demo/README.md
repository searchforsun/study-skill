# 阶段 1 示例：架构入门与本地运行

本目录提供 **Docker Compose** 拉起 **NameServer + Broker + Proxy** 的最小栈，以及使用官方推荐的 **`rocketmq-client-java`** 经 **Proxy** 完成一次「发送—消费」闭环的 Java 示例。编排与命令与官网「Docker Compose 快速开始」一致思路；Topic 创建方式遵循 5.x 对 **消息类型** 的约束说明。

## 示例总览

| 文件 / 目录 | 对应知识点 | 建议顺序 |
|------------|------------|----------|
| `docker-compose.yml` | NameServer、Broker、Proxy 角色与端口暴露 | 1 |
| `java-client/` | 5.x Java SDK、Proxy `endpoints`、Producer / PushConsumer | 3（在集群已启动且 Topic 已建之后） |

## 环境要求

- **操作系统**：64 位；已安装 **Docker** 与 **Docker Compose**（或 Docker Desktop 自带 compose）。
- **JDK**：**1.8+**（与官方 Quick Start 要求一致）；已安装 **Maven 3.6+**（用于编译运行 `java-client`）。
- **磁盘与内存**：本地学习默认单节点即可；若机器资源紧张，可先关闭其它容器再启动。

## 1. 启动 RocketMQ（Compose）

在**本目录**（含 `docker-compose.yml`）执行：

```powershell
docker compose up -d
```

启动后建议检查容器均为 `running`：

```powershell
docker ps --filter "name=rmq"
```

常见端口（与 compose 映射一致）：

- NameServer：**9876**
- Broker：**10911**（及 10909、10912 等，排障时可关注）
- Proxy：**8080**、**8081**（本阶段 Java 示例默认连接 **`localhost:8081`**）

停止与清理：

```powershell
docker compose down
```

## 2. 创建 Topic（5.x 建议显式声明消息类型）

进入 Broker 容器后使用 `mqadmin`。**5.x** 文档建议在创建 Topic 时附带 **`message.type`**，避免后续发送与 Topic 声明不一致。

在 **Windows PowerShell** 下可用一条命令执行（无需交互进入 bash）：

```powershell
docker exec rmqbroker sh mqadmin updateTopic -n rmqnamesrv:9876 -t TestTopic -c DefaultCluster -a +message.type=Normal
```

说明：

- **`-n`**：NameServer 地址；在 compose 网络内使用服务名 **`rmqnamesrv:9876`**。
- **`-t`**：Topic 名，需与 Java 示例中的默认 Topic 一致（`TestTopic`），或通过 JVM 参数覆盖（见下节）。
- **`-c`**：集群名；本镜像默认多为 **`DefaultCluster`**（若你改过 Broker 集群名，请以实际为准）。

若命令报错，请优先查看 **Broker** 与 **NameServer** 容器日志：

```powershell
docker logs rmqnamesrv --tail 200
docker logs rmqbroker --tail 200
```

## 3. 编译并运行 Java 示例

进入 `java-client/`：

```powershell
Set-Location java-client
mvn -q -DskipTests compile
```

**先启动消费者（在一个终端保持运行至收到消息）**，再开第二个终端发送：

```powershell
# 终端 A：先启动消费者（等待最多约 60 秒）
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage01.PushConsumerExample
```

```powershell
# 终端 B：发送一条消息
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage01.ProducerExample
```

若 Proxy 或 Topic 与默认不一致，**优先直接修改源码中的默认值**做实验，避免不同 Maven / 插件版本下 **JVM 系统属性** 传递方式不一致带来的困扰。示例类中已用 `System.getProperty("rmq.endpoints", "...")` 等形式留有扩展口，进阶用法可自行查阅 `exec-maven-plugin` 文档配置 `systemPropertyVariables`。

## 学习建议（如何改实验）

1. **先画后做**：对照 `THEORY.md` 中的拓扑图，标出 Producer、Proxy、Broker、NameServer 各步数据走向，再运行示例。
2. **故意失败以加深记忆**：未建 Topic 直接发送；或 Topic 的 `message.type` 与发送类型不匹配；观察异常信息与 Broker 日志差异。
3. **改 Tag 与 Key**：在 `ProducerExample` 修改 `setTag` / `setKeys`，在 `PushConsumerExample` 将过滤表达式从 `*` 改为指定 Tag，体会「订阅表达式」的位置感（后续阶段会系统讲过滤）。
4. **端口冲突**：本机若已占用 `9876` / `10911` / `8081` 等，修改 `docker-compose.yml` 左侧宿主机端口映射后，同步修改 `rmq.endpoints` 中的宿主机端口。

## 与官方文档的关系

- Compose 结构参考：RocketMQ 文档 **Run RocketMQ with Docker Compose**（见阶段 `THEORY.md` 文末推荐阅读）。
- Java 示例代码结构参考：同页中的 **Producer / PushConsumer** 片段；本仓库补充了中文注释与「先消费后发送」的课堂顺序。

若你更熟悉 **4.x 与 `rocketmq-client`（Classic）直连 NameServer** 的写法，可在读完本阶段理论后对照官方「迁移 / 多客户端」资料建立映射；本阶段刻意跟随 **5.x 官方 Compose + Proxy + `rocketmq-client-java`**，以减少与当前文档脱节。
