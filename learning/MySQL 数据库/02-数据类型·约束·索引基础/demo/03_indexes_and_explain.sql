-- 阶段 2：索引与 EXPLAIN 粗读（本阶段只建立直觉，优化细节见阶段 6）
-- 前置：已执行 01、02（表中已有少量数据；数据少时 rows 估计仅作演示）
-- 重复执行本文件若报「索引已存在」，可先：
--   DROP INDEX idx_product_title ON product;
--   DROP INDEX idx_product_cat_created ON product;

USE study_mysql_stage02;

-- 无合适索引时，按标题关键字查找可能走全表扫描（type 常为 ALL，见执行计划）
EXPLAIN SELECT * FROM product WHERE title LIKE '%MySQL%';

-- 为 title 前缀匹配或等值查询增加索引（按业务查询模式选型；LIKE '%xx%' 仍难用好 B-Tree）
CREATE INDEX idx_product_title ON product (title);

EXPLAIN SELECT * FROM product WHERE title LIKE 'MySQL%';

-- 复合索引：演示最左前缀 —— (category_id, created_at)
CREATE INDEX idx_product_cat_created ON product (category_id, created_at);

-- 能用到 category_id 作为最左列
EXPLAIN SELECT * FROM product WHERE category_id = 1 ORDER BY created_at DESC LIMIT 5;

-- 若条件里只有第二列，通常无法使用该复合索引的最左匹配（可对比 key 列是否为 NULL）
EXPLAIN SELECT * FROM product WHERE created_at > '2020-01-01';
