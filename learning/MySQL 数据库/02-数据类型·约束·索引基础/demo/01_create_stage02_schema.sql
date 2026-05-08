-- 阶段 2：练习库与三张关联表（分类 → 商品 → SKU）
-- 默认连接：与阶段 1 相同实例即可；本脚本创建独立库 study_mysql_stage02，避免覆盖阶段 1 数据。

CREATE DATABASE IF NOT EXISTS study_mysql_stage02
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE study_mysql_stage02;

-- 先删子表再删父表，避免外键顺序问题（重建演示环境时可重复执行）
DROP TABLE IF EXISTS product_sku;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;

-- 1) 商品分类：展示 SMALLINT 层级、CHECK（MySQL 8.0.16+）
CREATE TABLE product_category (
  id            SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code          VARCHAR(32)  NOT NULL COMMENT '业务侧稳定编码，全局唯一',
  name          VARCHAR(64)  NOT NULL,
  sort_order    INT          NOT NULL DEFAULT 0,
  level_hint    TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '仅供演示 CHECK，真实树结构后续再学',
  created_at    DATETIME(3)  NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_code (code),
  CONSTRAINT chk_category_level CHECK (level_hint BETWEEN 1 AND 9)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- 2) 商品：外键指向分类；金额用「分」整数；附属属性用 JSON 演示可选扩展字段
CREATE TABLE product (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id        SMALLINT UNSIGNED NOT NULL,
  title              VARCHAR(200) NOT NULL,
  list_price_cents   INT UNSIGNED NOT NULL COMMENT '标价（分），非负',
  is_digital         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 数字商品',
  extra_attrs        JSON         NULL COMMENT '演示 JSON：如 {"warranty_months":12}',
  created_at         DATETIME(3)  NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (id),
  KEY idx_product_category (category_id),
  CONSTRAINT fk_product_category FOREIGN KEY (category_id)
    REFERENCES product_category (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_price_nonneg CHECK (list_price_cents <= 999999999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主档';

-- 3) SKU：同一商品多规格；条码唯一；数量非负
CREATE TABLE product_sku (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id   BIGINT UNSIGNED NOT NULL,
  sku_code     VARCHAR(48) NOT NULL COMMENT '仓库/履约侧 SKU',
  barcode      VARCHAR(32)   NULL COMMENT '允许为空：部分场景暂无条码',
  stock_qty    INT           NOT NULL DEFAULT 0,
  updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sku_code (sku_code),
  KEY idx_sku_product (product_id),
  CONSTRAINT fk_sku_product FOREIGN KEY (product_id)
    REFERENCES product (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT chk_stock_nonnegative CHECK (stock_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SKU';
