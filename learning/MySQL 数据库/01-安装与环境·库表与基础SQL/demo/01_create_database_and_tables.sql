-- 阶段 1：建库与建表示例
-- 执行前请确保已能连接 MySQL，并具有创建库的权限（如 root 或授权账号）。

-- 若库已存在可跳过；字符集使用 utf8mb4 以兼容完整 Unicode（含 emoji）
CREATE DATABASE IF NOT EXISTS study_mysql_stage01
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE study_mysql_stage01;

-- 商品表：练习 INT / VARCHAR / DATETIME / DECIMAL 的基本用法
DROP TABLE IF EXISTS order_line;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS product;

CREATE TABLE product (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  sku_code      VARCHAR(32)  NOT NULL COMMENT '商品编码，业务唯一',
  name          VARCHAR(128) NOT NULL COMMENT '名称',
  price_cents   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '单价（分），避免浮点误差',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

-- 订单头：后面阶段会用来做 JOIN，此处仅建表
CREATE TABLE `order` (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_ref      VARCHAR(64)  NOT NULL COMMENT '练习用用户标识',
  status        VARCHAR(16)  NOT NULL DEFAULT 'NEW' COMMENT '订单状态',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_user (user_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单头';

-- 订单明细：外键到订单与商品，体会引用关系（约束细节在阶段 2 展开）
CREATE TABLE order_line (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id      BIGINT UNSIGNED NOT NULL,
  product_id    BIGINT UNSIGNED NOT NULL,
  qty           INT UNSIGNED NOT NULL DEFAULT 1,
  unit_price_cents INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '成交单价（分）',
  PRIMARY KEY (id),
  KEY idx_line_order (order_id),
  KEY idx_line_product (product_id),
  CONSTRAINT fk_line_order FOREIGN KEY (order_id) REFERENCES `order` (id),
  CONSTRAINT fk_line_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';
