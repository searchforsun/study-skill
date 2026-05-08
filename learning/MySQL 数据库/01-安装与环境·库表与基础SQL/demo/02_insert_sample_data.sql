-- 阶段 1：示例数据（依赖 01 已执行）
USE study_mysql_stage01;

-- 清空演示数据（开发练习库可接受；生产切勿照抄）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE order_line;
TRUNCATE TABLE `order`;
TRUNCATE TABLE product;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO product (sku_code, name, price_cents, is_active) VALUES
  ('SKU-BOOK-001', '动手学 MySQL', 5900, 1),
  ('SKU-MUG-001',  '陶瓷马克杯', 2999, 1),
  ('SKU-PEN-001',  '签字笔（黑）', 1500, 0);

-- 插入订单与明细：先订单头，后明细（外键要求被引用行已存在）
INSERT INTO `order` (user_ref, status) VALUES
  ('user-alice', 'PAID'),
  ('user-bob',   'NEW');

INSERT INTO order_line (order_id, product_id, qty, unit_price_cents) VALUES
  (1, 1, 1, 5900),
  (1, 2, 2, 2999),
  (2, 3, 5, 1500);
