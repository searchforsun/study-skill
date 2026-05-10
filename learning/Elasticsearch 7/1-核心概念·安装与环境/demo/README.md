# 阶段 1 示例说明：单节点集群与最小 CRUD 闭环

本目录用 Docker 启动 **Elasticsearch 7.17** 单节点，并通过脚本完成建索引、指定 ID 写入、读取、`_bulk` 批量写入、按 ID 删除与删除索引。**不演示** 生产级 TLS、认证、角色权限与跨集群复制；这些需在官方安全文档与团队规范下单独设计。

## 示例总览

| 入口文件 / 目录 | 对应知识点 | 建议顺序 |
|-----------------|-----------|----------|
| [`docker-compose.yml`](docker-compose.yml) | 单节点发现、堆内存、学习用安全开关 | 先启动 |
| [`scripts/crud.ps1`](scripts/crud.ps1) | REST 根路径、`cluster health`、索引/文档 CRUD、bulk、`_cat/indices` | Windows / PowerShell |
| [`scripts/crud.sh`](scripts/crud.sh) | 同上（Bash + `curl`） | Git Bash / WSL / Linux / macOS |

## 环境要求

- **Docker**：Docker Desktop 4.x+（Windows / macOS）或 Docker Engine 20.10+（Linux）
- **Docker Compose**：Compose V2（`docker compose` 子命令）
- **PowerShell**：Windows PowerShell 5.1+ 或 PowerShell 7+（脚本使用 `Invoke-RestMethod`）
- **Bash 脚本（可选）**：`curl`；若已安装 [`jq`](https://jqlang.github.io/jq/) 则输出格式化（未安装时可去掉脚本中的 `| jq .`）
- **操作系统**：Windows 10/11（本仓库首选 `crud.ps1`）；Linux 部署学习机若遇 `max virtual memory areas vm.max_map_count is too low`，按 [官方 Docker 文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docker.html) 调整宿主机内核参数

## 运行命令

### 1. 启动 Elasticsearch

在 **`demo` 目录**（与本 `README.md` 同级）执行：

```bash
docker compose up -d
```

等待集群就绪（首次拉镜像可能较慢）：

```bash
curl.exe -s http://127.0.0.1:9200/_cluster/health
```

### 2. Windows PowerShell：跑通 CRUD

```powershell
cd <本仓库>\learning\Elasticsearch 7\1-核心概念·安装与环境\demo
docker compose up -d
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\crud.ps1
```

**说明**：若 `Invoke-RestMethod` 报 SSL 或连接错误，确认 `docker compose ps` 中容器健康、`9200` 未被占用。

### 3. Bash：跑通 CRUD

```bash
cd "learning/Elasticsearch 7/1-核心概念·安装与环境/demo"
docker compose up -d
chmod +x scripts/crud.sh
./scripts/crud.sh
```

未安装 `jq` 时，可编辑 `scripts/crud.sh` 去掉 `| jq .` 管道，直接输出原始 JSON。

### 4. 停止并移除容器

```bash
docker compose down
```

## 学习建议（如何改代码做实验）

1. **`docker-compose.yml`**
   - 把 `ES_JAVA_OPTS` 从 `512m` 改为 `256m` 或 `1g`，观察启动日志与 OOM 风险（过小可能启动失败）。
   - 查阅官方文档后，尝试只改**副本数**相关索引设置（阶段 1 可先手写 `PUT` 索引时带 `settings`，观察 `/_cluster/health` 是否仍 `yellow`）。

2. **`scripts/crud.ps1` / `crud.sh`**
   - 暂时注释「删除索引」最后一步，用 Kibana Dev Tools 或 `curl` 反复 `GET /learning_stage1_demo/_search`（为阶段 3 预热）。
   - 修改 `_bulk` 中 `_id`，故意重复 `POST _bulk` 一次，对照返回里每条 `index` 的 `status` 与 `error`（若有）。
   - 自问：`GET /_cat/shards?v` 在本索引上与文档数量变化的关系是什么？答案应能在 **THEORY.md 第三节** 与官方 shards 介绍中对应。

## 与 `THEORY.md` 的配合

先阅读理论稿 **「一、节点与集群」**、**「三、REST API 习惯与健康检查」** 与 **「四、本地环境」**，再启动 `docker compose` 并运行 `crud.ps1`；将 `GET /`、`/_cluster/health`、`_bulk` 的响应作为现象锚点，回到文中对照 **单节点集群**、**green/yellow**、**`_doc` 路径** 的叙述。
