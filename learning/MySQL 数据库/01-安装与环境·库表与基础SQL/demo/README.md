# 阶段 1 示例说明（安装与环境 · 库表与基础 SQL）

## 示例总览

| 文件 | 知识点 | 建议顺序 |
|------|--------|----------|
| [`docker-compose.yml`](./docker-compose.yml) | 可选：Docker 启动 MySQL 8.4、`utf8mb4` | 无 Docker 可跳过 |
| [`01_create_database_and_tables.sql`](./01_create_database_and_tables.sql) | `CREATE DATABASE`、`CREATE TABLE`、主键/唯一/外键初识 | 1 |
| [`02_insert_sample_data.sql`](./02_insert_sample_data.sql) | `INSERT`、外键顺序、`TRUNCATE` | 2 |
| [`03_basic_queries.sql`](./03_basic_queries.sql) | `WHERE`/`ORDER BY`/`LIMIT`、简单 `JOIN` | 3 |

第 3 个文件末尾包含 **两表连接**（`order_line` + `order` + `product`），用于提前感知「多表查询」形态；语法细节会在阶段 3 系统讲解，此处可读 SQL 结构与结果即可。

## 环境要求

- **MySQL**：建议 **8.4 LTS** 或与手册一致的 8.x（与 [`THEORY.md`](../THEORY.md) 推荐阅读对齐）。
- **客户端**：任选其一——官方 `mysql` 客户端、MySQL Shell、Workbench、DBeaver 等。
- **可选**：已安装 **Docker Desktop**（Windows）用于启动 `docker-compose.yml`。

## 方式 A：使用已有 MySQL 实例

1. 使用具备建库权限的账号连接（示例使用命令行）：

   ```bash
   mysql -h 127.0.0.1 -P 3306 -u root -p
   ```

2. 在客户端中依次执行（或 `SOURCE`）：

   ```bash
   SOURCE /path/to/01_create_database_and_tables.sql;
   SOURCE /path/to/02_insert_sample_data.sql;
   SOURCE /path/to/03_basic_queries.sql;
   ```

   Windows 下路径写成 `SOURCE D:/project/study-demo/learning/MySQL 数据库/01-安装与环境·库表与基础SQL/demo/01_create_database_and_tables.sql;` 这类绝对路径通常更省事。

3. 预期：`study_mysql_stage01` 库中存在 `product`、`order`、`order_line` 三张表，且 `03_basic_queries.sql` 能返回多组结果集。

## 方式 B：Docker Compose（可选）

在 **`demo/` 目录**（与本文件同级）执行：

```bash
docker compose up -d
```

默认把容器 **3306** 映射到本机 **3306**，root 密码为 compose 文件中的 `MYSQL_ROOT_PASSWORD`（**请在本地修改为强密码**）。

连接示例：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p
```

然后按 **方式 A** 执行三个 SQL 文件。首次启动数据库初始化可能需要数十秒，若连接失败可稍等再试。

停止并删除容器（数据卷是否删除取决于你是否要保留练习数据）：

```bash
docker compose down
```

## 推荐修改实验

- 在 `product` 表新增一行你自己的商品数据，再跑 `03_basic_queries.sql` 的前半部分，观察 `WHERE` / `ORDER BY` 的变化。
- 尝试把 `03_basic_queries.sql` 第 1 题中的 `LIMIT 5` 改成 `LIMIT 1 OFFSET 1`，体会分页偏移。
- **故意**写一条无 `WHERE` 的 `UPDATE`（在备份或可丢弃的实验库中），再用 `SELECT` 查看影响范围——用于建立风险意识（勿在生产操作）。

## 常见问题

- **外键错误**：先插入 `product` 与 `order`，再插入 `order_line`；顺序见 `02_insert_sample_data.sql`。
- **保留字**：本示例订单表名为 `` `order` ``（反引号包裹），因为 `ORDER` 与排序关键字冲突。
- **字符集**：库表均使用 `utf8mb4`，避免中文与 emoji 存储异常（详见后续阶段）。
