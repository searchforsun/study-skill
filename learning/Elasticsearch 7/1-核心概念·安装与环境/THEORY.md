# 阶段 1：核心概念·安装与环境

> 与 [`ROADMAP.md`](../ROADMAP.md) 阶段 1 对齐。动手印证见本目录 [`demo/README.md`](demo/README.md)。**生成说明**：推荐阅读链接基于官方 7.17 Reference 入口撰写；镜像标签与默认值持续演进，**请以 [7.17 指南](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/index.html) 与团队规范为准**。

本阶段要在本地建立「集群 → 索引 → 文档」的可操作心智模型，并养成用 `cluster health` 与 `_cat` 先看状态、再写 DSL 的习惯；这是阶段 2 映射与阶段 3 查询的共同前提。

## 本阶段知识地图

| 块 | 你要带走的抓手 |
|----|----------------|
| 一 | 节点与集群：单节点也是集群，先理解协调再谈扩容 |
| 二 | 索引与文档：`_id`、`_doc` 路径习惯与 7.x 单类型约束 |
| 三 | REST 与健康检查：动词与路径模式、`green/yellow/red` 的判读 |
| 四 | 本地环境：Docker 单节点关键变量与最小 CRUD 闭环 |

**路线要点 ↔ 本文章节**

| `ROADMAP.md` 阶段 1 要点 | 本文展开位置 |
|--------------------------|----------------|
| 节点与集群 | **一** |
| 索引与文档 ID | **二** |
| REST API 习惯；`cluster health` 与 `_cat` | **三** |
| Docker/本机安装与 CRUD 实践 | **四** |

---

## 一、节点与集群：单节点也是集群

Elasticsearch 对外始终呈现为「一个可以对话的集群端点」，即便你只在笔记本上跑了一个进程。**先把「集群」理解成协调单元，而不是机器台数**，后续学习分片与副本时才不会把术语学成死记硬背。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | 节点（node） | 一个 ES 进程实例；承担数据、协调等角色（阶段 5 再展开角色细分） |
| 2 | 集群（cluster） | 共享 `cluster.name`、由集群状态协调的一组节点 |
| 3 | 单节点集群 | `discovery.type=single-node` 时常用；**仍是完整集群语义** |

### 1.1 结论：不要等「多台机器」才开始用集群视角

**单节点部署的学习价值在于：你已经在与「集群状态、REST 入口、索引命名空间」打交道。** 若始终把 ES 当成「本机里的一个数据库进程」，容易忽略分配、路由、健康度这些分布式语义；反过来，若一开始就用集群视角读 `_cluster/health` 与 `_cat/nodes`，阶段 5 的分片与副本会自然接上。

例如你在本机只启动一个容器，请求 `GET /` 仍能看到 `cluster_name` 与 `cluster_uuid`；这与生产多节点在「接口形状」上是一致的，差别主要在规模与容错。具象实验见 `demo/README.md` 中「集群根信息」一步。

### 1.2 衔接：协调、路由与后续阶段的「伏笔」

**写入与查询并不是「直接打开某个文件」**，而是由集群状态参与路由（哪份主分片承接写入、从哪些副本可读）。你现在不必掌握路由算法，但要建立预期：**同一 REST 路径在不同健康状态下行为可能不同**（例如副本未分配时集群可能长期 `yellow`）。这与「先 health 再业务请求」的习惯直接相关；下文第三节会把健康度读成可行动信号。

---

**本节提要（延伸学习）**

- **核心概念**：节点；集群；cluster.name；单节点发现；集群状态（概念层）
- **拓展提问提示词**

> 主题：Elasticsearch 节点与集群的基本语义。核心概念：节点、集群、cluster.name、单节点发现、集群状态。请拓展：1）单节点集群与三节点集群在「故障容错」上的差异是什么？2）`cluster.name` 若在不同环境重复可能引发什么问题？3）官方文档中「cluster state」包含哪些大类信息（仅列目录级）？

---

## 二、索引、文档与 7.x 路径习惯

索引是文档的逻辑集合与配置（映射、设置）的挂载点；文档由 `_id` 标识。**7.x 已移除多 mapping type 的生产用法，REST 路径上常见 `_doc` 作为统一端点**——你要习惯「索引名 + `_doc` + 可选 ID」这一形状，而不是背旧版 type 名称。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | 索引（index） | 名字、设置与映射的边界；阶段 2 深入 mapping |
| 2 | 文档（document） | JSON 对象；`_id` 可自动生成或显式指定 |
| 3 | `_doc` | 7.x 推荐的类型无关写入/读取路径组件 |

### 2.1 文档 ID：显式指定与自动生成

**显式 `PUT /my-index/_doc/1` 适用于幂等写入与「业务主键即文档 ID」模型；`POST /my-index/_doc` 让 ES 生成 `_id`，适合日志型追加。** 两者在冲突语义与排错路径上不同：显式 ID 重复写入会覆盖（在默认索引策略下）；自动生成则每次新增一条。学习阶段建议两种都试一遍，并在 `GET /my-index/_doc/<id>` 里对照 `_version` 与 `result` 字段。

本阶段 demo 中同时包含「指定 ID 单条写入」与「`_bulk` 批量写入」，作为具象锚点。

### 2.2 7.x 单类型与 REST 路径：少记术语、多记形状

**不必再为业务构造多个 type 名称；以索引为边界组织数据更符合 7.x 实践。** 路径上见到 `_doc` 应理解为「类型占位兼容层」，真正决定字段行为的是 mapping（下一阶段）。若从旧资料复制 `/{index}/{type}/_search`，要警觉：7.x 文档与客户端多数已迁移到 `_doc` 或无 type 形态。

| 旧直觉 | 7.x 学习阶段更稳的习惯 |
|--------|-------------------------|
| type 当「表」 | 一个业务对象一类索引（或别名策略），用字段与别名切分 |
| 只记 DSL | 同步记「这个字段在 mapping 里是什么类型」 |

---

**本节提要（延伸学习）**

- **核心概念**：索引；文档；`_id`；`_doc`；幂等写入与自动生成 ID
- **拓展提问提示词**

> 主题：索引、文档标识与 7.x `_doc` 路径。核心概念：索引、文档、_id、_doc、显式 ID 与 POST 自动生成。请拓展：1）同一 `_id` 连续两次 index 请求，响应里哪些字段能体现写入结果变化？2）为何 7.x 强调单类型映射，对索引命名与别名策略有什么影响？3）官方文档中「document metadata」常用字段有哪些？

---

## 三、REST API 习惯与健康检查

Elasticsearch 的主流接口是 HTTP + JSON。**把「路径含义、动词幂等性、返回 JSON 结构」练成肌肉记忆**，比提前堆查询技巧更能减少低级错误。入门阶段优先掌握：根信息、`/_cluster/health`、`_cat` 只读诊断、索引与文档的 CRUD。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | 动词与幂等 | `PUT` 建索引/带 ID 写入；`POST` 搜索与 bulk；`DELETE` 删除 |
| 2 | `cluster health` | `status`、`number_of_nodes`、`unassigned_shards` |
| 3 | `_cat` API | 人类可读表格；学习期比纯 JSON 更快建立空间感 |

### 3.1 路径与动词：从「资源」角度读 URL

**把 `/my-index` 当成索引资源、`/my-index/_doc/1` 当成具体文档资源**，DELETE/GET/PUT 的语义就与常见 REST 资源风格一致。搜索与 bulk 多走 `POST`，因为请求体往往较长且不必缓存为「同一 URL 的幂等 GET」。当你看到 `POST /my-index/_search`，要意识到：这是「提交查询程序」，不是简单子资源创建。

短例：`GET /_cat/indices?v` 快速扫一眼索引占用与健康关联；`GET /my-index/_doc/1` 验证某条文档是否存在。完整命令序列见 `demo/scripts/`。

### 3.2 `cluster health` 与 `_cat`：先体检再调参

**`green` 表示主分片与所需副本都就绪；`yellow` 常见於单节点副本无法分配；`red` 表示有主分片不可用。** 学习期看到 `yellow` 不必惊慌，但要能解释：「我是不是只有单节点却建了副本？」这与路线中的反模式「跳过 health 直接背 DSL」相对照——**没有健康度上下文，很多 DSL 现象无法判因**。

`/_cat/shards?v` 与 `/_cat/nodes?v` 在阶段 5 会反复使用；阶段 1 先建立「看一眼 shards 分布」的习惯即可。

---

**本节提要（延伸学习）**

- **核心概念**：REST 路径；幂等；`_cluster/health`；green/yellow/red；`_cat`
- **拓展提问提示词**

> 主题：Elasticsearch REST 习惯与集群健康诊断入门。核心概念：REST 路径、动词幂等、cluster health、green/yellow/red、_cat API。请拓展：1）单节点环境下集群为何可能长期 `yellow`，是否与副本数有关？2）`unassigned_shards` 非零时，你优先查阅官方文档哪一类章节？3）`_cat` 与对应 JSON API 各适合什么场景？

---

## 四、本地环境：Docker 单节点与最小闭环

阶段 1 的实践目标是：**用可重复的方式启动 7.17 单节点，完成建索引、写入、读取、批量写入、删除，并能在 health/`_cat` 上读出与操作对应的变化。** 官方同时提供压缩包安装与 Docker；本仓库 demo 走 Docker，以便对齐 `ROADMAP.md` 中的实践描述。

| # | 知识点 | 抓住什么 |
|---|--------|----------|
| 1 | `discovery.type=single-node` | 开发用单节点集群的常用开关 |
| 2 | 堆内存 `ES_JAVA_OPTS` | 本机内存与容器 OOM 的第一道门槛 |
| 3 | 安全与网络 | 学习可用简化配置；生产需按官方安全模型收敛 |

### 4.1 Docker 关键环境变量：够用、可解释

**`discovery.type=single-node` 告诉节点不要按生产多播去发现同伴；`ES_JAVA_OPTS=-Xms512m -Xmx512m` 控制堆大小，避免默认过大吃光本机内存。** 镜像建议使用 Elastic 官方仓库 `docker.elastic.co/elasticsearch/elasticsearch:7.17.x`（补丁版本以团队镜像策略为准）。若拉取失败或需固定补丁号，见 `demo/docker-compose.yml` 注释。

Linux 宿主机若遇内存映射限制，官方文档会提示调整 `vm.max_map_count`；Windows Docker Desktop 多数场景可忽略，但若迁移到 WSL2/Linux 服务器需补这一课。

### 4.2 反模式：跳过可观测性、复制生产级网络与安全默认值

**不要在未读 `network.host`、TLS、认证模型的情况下，把学习用 `xpack.security.enabled=false` 的配置套到公网可达主机。** 路线强调的反模式还包括：从不看 `_cat`/`health` 直接堆复杂查询——这会导致排错时缺少「分片是否分配、索引是否存在」的基础事实。

本阶段闭环以 demo 脚本为准；完成后再回到第一节用「集群视角」复述你每一步操作改变了什么集群对象。

---

**本节提要（延伸学习）**

- **核心概念**：discovery.type=single-node；ES_JAVA_OPTS；官方 Docker 镜像；学习环境与生产安全边界
- **拓展提问提示词**

> 主题：Elasticsearch 7.17 本地 Docker 单节点与最小 CRUD 闭环。核心概念：discovery.type、ES_JAVA_OPTS、官方镜像、xpack.security 学习配置与生产差异、vm.max_map_count。请拓展：1）单节点下如何把副本数设为 0 以避免长期 yellow（官方对开发环境的建议口径是什么）？2）从 7.17 迁往 8.x 时，安全默认值变化对学习脚本有什么影响？3）你如何在 CI 中复现「可重复的最小 ES 集成测试环境」？

---

## 推荐阅读

> 说明在前、链接行仅 URL（复制时不会夹带额外字符）。

- **Elasticsearch Reference 7.17 — What is Elasticsearch?**
  - 关联主题：产品定位、倒排索引直觉、与 Lucene 的关系（概念层）。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/elasticsearch-intro.html
  - 检索：`Elasticsearch 7.17 what is elasticsearch`

- **Elasticsearch Reference 7.17 — Getting started**
  - 关联主题：索引与文档、基本 CRUD 与查询入门路径。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/getting-started.html
  - 检索：`Elasticsearch 7.17 getting started`

- **Elasticsearch Reference 7.17 — Install Elasticsearch**
  - 关联主题：安装选项、与本机环境的依赖与注意点。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/install-elasticsearch.html
  - 检索：`Elasticsearch 7.17 install`

- **Elasticsearch Reference 7.17 — Install Elasticsearch with Docker**
  - 关联主题：`docker run`/`compose` 环境变量、生产与开发的边界说明。
  - https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docker.html
  - 检索：`Elasticsearch 7.17 docker install`

- **Elastic Support and Product Release EOL Policy**
  - 关联主题：7.x 维护状态与升级规划入口。
  - https://www.elastic.co/support/eol
  - 检索：`Elastic product end of life policy`
