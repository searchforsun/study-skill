-- 阶段 2：约束行为演示（CHECK、外键、唯一冲突）
-- 前置：已执行 01_create_stage02_schema.sql

USE study_mysql_stage02;

-- ---------- 合法样本数据 ----------
INSERT INTO product_category (code, name, sort_order, level_hint)
VALUES
  ('cat-books', '图书', 10, 1),
  ('cat-digital', '数字内容', 20, 1);

INSERT INTO product (category_id, title, list_price_cents, is_digital, extra_attrs)
VALUES
  (1, 'MySQL 学习笔记（纸质）', 5990, 0, JSON_OBJECT('pages', 320)),
  (2, '在线视频课（解锁码）', 19900, 1, JSON_OBJECT('duration_hours', 12));

INSERT INTO product_sku (product_id, sku_code, barcode, stock_qty)
VALUES
  (1, 'BOOK-MYSQL-001', '6901234567890', 120),
  (2, 'VID-COURSE-001', NULL, 9999);

-- ---------- CHECK：超出范围应失败（期望报错，勿改脚本强行「成功」）----------
-- 下一行若执行成功，请检查 MySQL 版本是否 >= 8.0.16 且存储引擎为 InnoDB。
-- INSERT INTO product_category (code, name, level_hint) VALUES ('x','y', 99);

-- ---------- 外键：引用不存在的分类 ----------
-- INSERT INTO product (category_id, title, list_price_cents)
-- VALUES (9999, '非法分类', 100);

-- ---------- 唯一约束：重复 sku_code ----------
-- INSERT INTO product_sku (product_id, sku_code, stock_qty) VALUES (1, 'BOOK-MYSQL-001', 0);

-- ---------- 查看 JSON 列（结果集因客户端而异）----------
SELECT id, title, extra_attrs, extra_attrs->>'$.pages' AS pages FROM product WHERE id = 1;
