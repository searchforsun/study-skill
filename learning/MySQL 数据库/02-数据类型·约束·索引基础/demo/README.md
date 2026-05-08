# 阶段 2 示例说明：`数据类型 · 约束 · 索引基础`

本目录脚本在独立库 **`study_mysql_stage02`** 中演示：类型选择（含 `JSON`）、`CHECK`/`FOREIGN KEY`/`UNIQUE`、单列与复合索引及 **`EXPLAIN` 粗读**。默认假设你已能连接 MySQL（与阶段 1 相同工具即可）。

## 环境要求

- MySQL **8.0.16+**（用于 `CHECK` 约束；推荐与路线一致的 **8.4**）。
- 客户端：`mysql` 命令行、MySQL Shell、或任意图形客户端。
- 若使用 Docker：可继续用阶段 1 的 [`../01-安装与环境·库表与基础SQL/demo/docker-compose.yml`](../01-安装与环境·库表与基础SQL/demo/docker-compose.yml) 启动实例，本阶段只需在同一实例上执行脚本创建新库。

## 文件概览与推荐顺序

| 顺序 | 文件 | 内容要点 |
|------|------|----------|
| 1 | `01_create_stage02_schema.sql` | 创建 `study_mysql_stage02`；三张表：`product_category` → `product` → `product_sku`，含外键与 `CHECK` |
| 2 | `02_constraint_demonstrations.sql` | 插入合法样本数据；内含「应失败」的 SQL（注释形式），可自行取消注释观察报错 |
| 3 | `03_indexes_and_explain.sql` | `CREATE INDEX`；多条 `EXPLAIN`，体会有无索引、复合索引最左前缀 |

## 命令示例

在仓库根目录或本目录下，将连接参数换成你的主机、用户与密码：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < "01_create_stage02_schema.sql"
mysql -h 127.0.0.1 -P 3306 -u root -p < "02_constraint_demonstrations.sql"
mysql -h 127.0.0.1 -P 3306 -u root -p < "03_indexes_and_explain.sql"
```

Windows PowerShell 同样可用重定向；或在图形客户端中按顺序打开文件执行。

## 建议动手实验

1. 在 `02_constraint_demonstrations.sql` 中**取消注释**「非法 `category_id`」的 `INSERT`，阅读报错信息并对照 [`THEORY.md` 中外键小节](../THEORY.md)。
2. 修改 `chk_category_level` 允许的层级范围，再插入边界数据，观察 `CHECK` 何时拒绝写入。
3. 对 `03_indexes_and_explain.sql` 中最后一条查询：尝试改为同时包含 `category_id` 与 `created_at` 条件，对比 `EXPLAIN` 中 `key` 与 `type` 的变化。
4.（扩展）在 `product` 表增加一列 `FULLTEXT` 不适用的场景思考：为何电商标题搜索常交给搜索引擎而非仅靠 B-Tree —— 仅作课外思考，本路线阶段 6 再系统讲优化。

## 与阶段 1 的关系

- 阶段 1 练习库 **`study_mysql_stage01`** 可保留；本阶段使用 **`study_mysql_stage02`**，互不覆盖。
- 若需清空本阶段重来：可 `DROP DATABASE study_mysql_stage02;` 后从 `01` 脚本重新执行。
