# 阶段 1 示例：架构入门与本地运行

本目录配合上级文件夹中的 [`THEORY.md`](../THEORY.md)，完成 **Docker 启动 NameServer + Broker（含 Proxy）→ 创建 Topic → Java 客户端收发** 的最小闭环。

## 示例总览

| 文件 / 目录 | 对应知识点 | 建议顺序 |
|-------------|------------|----------|
| `docker-compose.yml` | NameServer、Broker、`--enable-proxy`、端口映射 | 1 |
| `broker.conf` | `brokerIP1` 与宿主机访问方式 | 1 |
| `java-client/` | `rocketmq-client-java`、Endpoint（Proxy）、Producer / PushConsumer | 2～3 |

## 环境要求

- **Docker Desktop**（或兼容的 Docker 引擎），并已启用 Compose。
- **64 位 JDK 17+**（与 `java-client/pom.xml` 中 `maven.compiler.release` 一致；若仅运行容器可不装 JDK）。
- **Apache Maven 3.9+**（用于编译运行 Java 示例）。

## 1. 启动 RocketMQ

在 **本 `demo/` 目录** 下执行：

```powershell
docker compose up -d
```

确认：

- `docker logs rmqnamesrv` 末尾附近出现类似 **`The Name Server boot success`** 的日志。
- `docker logs rmqbroker` 或容器内 `proxy.log` / `broker.log` 出现 **`boot success`**（具体文件名以镜像为准）。

停止：

```powershell
docker compose down
```

## 2. 创建 Topic

官网 Docker 教程使用 `TestTopic`。在 Broker 容器内执行（集群名 `DefaultCluster` 为默认常见写法）：

```powershell
docker exec -it rmqbroker sh mqadmin updatetopic -n namesrv:9876 -t TestTopic -c DefaultCluster
```

若命令报错，请把终端完整输出保存下来，并对照官方文档核对 **Broker 是否已向 NameServer 注册**、**网络别名 `namesrv` 是否可达**（本 Compose 文件中 NameServer 服务名为 `namesrv`）。

## 3. 运行 Java 客户端

进入 `java-client/`：

```powershell
cd java-client
mvn -q compile
```

**推荐调试顺序**（先消费端再等发送，避免消息已发完才开始监听）：

1. 在一个终端启动消费者（会最多等待 60 秒收一条消息）：

   ```powershell
   mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage01.PushConsumerExample
   ```

2. 在另一个终端发送一条消息：

   ```powershell
   mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage01.ProducerExample
   ```

消费者终端应打印 **messageId** 与消息体；生产者终端打印 **发送成功**。

### 可选 JVM 参数

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `rocketmq.endpoints` | `localhost:8081` | 与 Docker 映射的 **Proxy** 端口一致（官网示例常用 8081） |
| `rocketmq.topic` | `TestTopic` | 需与 `mqadmin` 创建的 Topic 一致 |
| `rocketmq.consumerGroup` | `DemoConsumerGroup` | 消费者分组名 |

示例：

```powershell
mvn -q exec:java -Dexec.mainClass=com.study.rocketmq.stage01.ProducerExample `
  -Drocketmq.endpoints=localhost:8081 `
  -Drocketmq.topic=TestTopic
```

## 4. 建议的动手实验

- 修改 `ProducerExample` 中消息体字符串，观察消费者输出变化。
- 故意 **不启动** NameServer 或 Broker，分别观察客户端报错，建立「现象 ↔ 组件」的对应关系。
- 阅读容器日志中 **注册 / 路由 / 代理** 相关关键词，和 [`THEORY.md`](../THEORY.md) 中的组件图对照。

## 5. 常见问题

- **连接被拒绝（8081）**：确认 `docker compose ps` 中端口已映射，且 Broker 带 `--enable-proxy` 启动。
- **发送成功但消费者一直等待**：Topic 是否创建、消费者与生产者是否使用同一 `topic`、是否先启消费者再发（或改用持久订阅 / 重复发送，详见后续阶段）。
- **路径错误**：升级 `apache/rocketmq` 镜像大版本时，`broker.conf` 在容器内的挂载路径可能变化，请以官方对应版本的 Docker 文档为准修改 `docker-compose.yml` 中的卷路径。

更多权威说明见官网：[Run RocketMQ in Docker](https://rocketmq.apache.org/docs/quickStart/02quickstartWithDocker/)。
