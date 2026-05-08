-- 阶段 1：基础查询练习（依赖 02 已导入数据）
USE study_mysql_stage01;

-- 1) 查看上架商品，按价格从高到低，取前 5 条
SELECT id, sku_code, name, price_cents
FROM product
WHERE is_active = 1
ORDER BY price_cents DESC
LIMIT 5;

-- 2) 模糊查询名称中包含「笔」的商品
SELECT sku_code, name, price_cents
FROM product
WHERE name LIKE '%笔%';

-- 3) 价格在区间 [2000, 5000] 分之间的商品
SELECT name, price_cents
FROM product
WHERE price_cents BETWEEN 2000 AND 5000;

-- 4) NULL 练习：假设某列允许 NULL 时，应使用 IS NULL / IS NOT NULL
-- 本示例表暂无业务 NULL 列，仅保留模板语句备查：
-- SELECT * FROM product WHERE some_optional_col IS NULL;

-- 5) 订单明细展开：列出每笔明细的行金额（qty * unit_price_cents）
SELECT
  ol.id            AS line_id,
  o.id             AS order_id,
  o.user_ref,
  p.name           AS product_name,
  ol.qty,
  ol.unit_price_cents,
  (ol.qty * ol.unit_price_cents) AS line_amount_cents
FROM order_line AS ol
JOIN `order` AS o ON o.id = ol.order_id
JOIN product AS p ON p.id = ol.product_id
WHERE o.status = 'PAID'
ORDER BY o.id, ol.id;
