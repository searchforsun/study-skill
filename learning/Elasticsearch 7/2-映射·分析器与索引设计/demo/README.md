# 阶段 2 示例说明：映射·分析器与索引设计

本目录示例验证：显式 mapping 如何定义字段类型与参数、分析器管道的组成、text vs keyword 的索引路径差异、`copy_to` 与 `multi_match` 的对比，以及 `dynamic: strict` 如何保护 schema。不演示：多节点集群分片细节（阶段 5）、复杂 DSL 查询（阶段 3）。

## 示例总览

| 入口文件 / 目录 | 对应知识点 | 建议顺序 |
|----------------|-----------|----------|
| `scripts/mapping-experiments.sh` | 完整实验：分析器对比 → 显式 mapping → 文档写入 → text/keyword 查询 → copy_to → 聚合 → strict 拒绝 | 先运行 |
| `scripts/mapping-experiments.ps1` | 同上（Windows PowerShell 版） | 按需 |
| `docker-compose.yml` | 单节点 7.17 ES（端口 9201，避免与阶段 1 端口冲突） | 前置 |

## 环境要求

- **Docker**：Docker Desktop 或 Docker Engine 20.10+
- **curl**：7.x+（shell 脚本使用）
- **PowerShell**：5.1+（`.ps1` 脚本使用）
- **操作系统**：Windows / macOS / Linux 均可
- **端口**：确保 `9201` 未被占用（若冲突，修改 `docker-compose.yml` 中的 `ports` 映射）

## 运行命令

### 1. 启动 ES

```bash
docker-compose up -d
# 等待约 10–30 秒，通过 healthcheck 自动变为 healthy
docker-compose ps
```

### 2. 执行实验脚本

**Linux / macOS / Git Bash**：

```bash
bash scripts/mapping-experiments.sh
```

**Windows PowerShell**：

```powershell
.\scripts\mapping-experiments.ps1
```

> 若 PowerShell 提示 `running scripts is disabled`，先执行：
> ```powershell
> Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
> ```

### 3. 停止并清理

```bash
docker-compose down -v   # -v 删除数据卷，下次实验从零开始
```

## 学习建议（如何改代码做实验）

1. **分析器对比**
   - 修改 `scripts/mapping-experiments.sh` 中 `=== 2.` 的 `TEXT` 变量，输入中文 "我爱学习 Elasticsearch"，对比 `standard`、`keyword` 和 `whitespace` 的分词差异。
   - 在 `_analyze` 请求中把 `analyzer` 换成 `stop` 查看停用词移除效果。
   - 自问：为什么中文短语在 standard 分析器下每个字都是独立 token？

2. **显式 mapping 参数调整**
   - 将 `title` 的 `norms` 改为 `false`，重建索引（`DELETE /articles` 后重新 PUT），对比相同查询下 `_score` 的变化。
   - 将 `summary` 的 `norms` 改成 `true`，观察评分排序的变化。
   - 新增一个字段 `source_url`（type: keyword, index: false），验证搜索不到但 `_source` 能取回。

3. **text vs keyword**
   - 把 `=== 6.2` 的 `term` 查询用在 `title`（text 类型）而非 `title.keyword` 上，观察结果为空的原因。
   - 在 `=== 6.1` 中搜索 `"Mapping"`（大写 M），结果中有没有命中文档？为什么？（提示：my_article_analyzer 包含 lowercase filter）
   - 自问：如果你的搜索框既能做模糊匹配又能做精确筛选，应该在 mapping 里做什么？

4. **copy_to vs multi_match**
   - 观察 `=== 7.` 和 `=== 8.` 的结果数量——应当一致。在 `_source` 中查找 `full_search` 字段——你会发现它不在返回结果中（`copy_to` 不存 `_source`）。
   - 思考：如果 `tags` 是 keyword 类型，它被 `copy_to` 到 `full_search`（text 类型）后经历了什么？

5. **dynamic: strict**
   - 将 mapping 中 `dynamic` 改为 `true`，重新创建索引，再次运行 `=== 11.` 的请求——`unknown_field` 会被自动添加。然后 `GET /articles/_mapping` 观察 ES 为它推断了什么类型。
   - 再改为 `false`——字段不会被索引但仍存在 `_source` 中。用 `match` 查询 `unknown_field` 验证搜索不到。

## 与 `THEORY.md` 的配合

先阅读理论稿 **「一、映射基础」** 与 **「二、text vs keyword」**，再运行脚本中 `=== 2.` 到 `=== 6.`。然后读 **「三、分析器」** 与 **「四、索引参数取舍」**，对照脚本 `=== 3.` 中 `my_article_analyzer` 的配置理解 analyzer 管道。最后读 **「五、索引设计实战」**，把整个 mapping 设计流程与脚本中的创建、写入、查询步骤对应起来，体会「需求→字段→类型→参数」的决策链条。
