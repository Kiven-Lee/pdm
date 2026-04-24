-- ============================================================
-- 商城项目数据库初始化脚本
-- 执行顺序：依次执行各数据库的建表语句
-- ============================================================

-- ===== 认证数据库 =====
CREATE DATABASE IF NOT EXISTS mall_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall_auth;

-- 用户表
CREATE TABLE IF NOT EXISTS mall_user (
    id          BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名（唯一）',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=正常，1=禁用',
    create_time DATETIME     NOT NULL COMMENT '创建时间（自动填充）',
    update_time DATETIME     NOT NULL COMMENT '更新时间（自动填充）',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试用户（密码：123456，BCrypt 加密）
INSERT INTO mall_user (id, username, password, status, create_time, update_time, deleted)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 0, NOW(), NOW(), 0);


-- ===== 商品数据库 =====
CREATE DATABASE IF NOT EXISTS mall_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall_product;

-- 商品分类表
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT      NOT NULL COMMENT '主键',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类 ID，顶级分类为 0',
    level       TINYINT     NOT NULL DEFAULT 1 COMMENT '层级：1=一级，2=二级，3=三级',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    icon        VARCHAR(255) DEFAULT NULL COMMENT '分类图标 URL',
    create_time DATETIME    NOT NULL,
    update_time DATETIME    NOT NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS product (
    id           BIGINT         NOT NULL COMMENT '主键',
    name         VARCHAR(200)   NOT NULL COMMENT '商品名称',
    description  TEXT           DEFAULT NULL COMMENT '商品描述',
    price        DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    stock        INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    category_id  BIGINT         NOT NULL COMMENT '所属分类 ID',
    main_image   VARCHAR(255)   DEFAULT NULL COMMENT '商品主图 URL',
    images       TEXT           DEFAULT NULL COMMENT '商品图片列表（JSON 数组）',
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：0=下架，1=上架',
    sales        INT            NOT NULL DEFAULT 0 COMMENT '销量',
    create_time  DATETIME       NOT NULL,
    update_time  DATETIME       NOT NULL,
    deleted      TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 插入测试分类
INSERT INTO category (id, name, parent_id, level, sort, create_time, update_time, deleted) VALUES
(1, '手机数码', 0, 1, 1, NOW(), NOW(), 0),
(2, '电脑办公', 0, 1, 2, NOW(), NOW(), 0),
(3, '服装鞋包', 0, 1, 3, NOW(), NOW(), 0),
(11, '手机', 1, 2, 1, NOW(), NOW(), 0),
(12, '平板电脑', 1, 2, 2, NOW(), NOW(), 0),
(21, '笔记本电脑', 2, 2, 1, NOW(), NOW(), 0);

-- 插入测试商品
INSERT INTO product (id, name, description, price, stock, category_id, main_image, status, sales, create_time, update_time, deleted) VALUES
(1001, 'iPhone 15 Pro', '苹果最新旗舰手机，A17 Pro 芯片', 7999.00, 100, 11, 'https://example.com/iphone15.jpg', 1, 500, NOW(), NOW(), 0),
(1002, '华为 Mate 60 Pro', '华为旗舰手机，麒麟 9000S 芯片', 6999.00, 200, 11, 'https://example.com/mate60.jpg', 1, 800, NOW(), NOW(), 0),
(1003, 'MacBook Pro 14', '苹果笔记本，M3 Pro 芯片', 14999.00, 50, 21, 'https://example.com/macbook.jpg', 1, 200, NOW(), NOW(), 0);


-- ===== 订单数据库 =====
CREATE DATABASE IF NOT EXISTS mall_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall_order;

-- 订单表
CREATE TABLE IF NOT EXISTS mall_order (
    id           BIGINT         NOT NULL COMMENT '主键',
    order_no     VARCHAR(30)    NOT NULL COMMENT '订单编号（业务唯一标识）',
    user_id      BIGINT         NOT NULL COMMENT '下单用户 ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '状态：0=待支付，1=已支付，2=已发货，3=已完成，4=已取消',
    pay_time     DATETIME       DEFAULT NULL COMMENT '支付时间',
    ship_time    DATETIME       DEFAULT NULL COMMENT '发货时间',
    finish_time  DATETIME       DEFAULT NULL COMMENT '完成时间',
    cancel_time  DATETIME       DEFAULT NULL COMMENT '取消时间',
    address      TEXT           DEFAULT NULL COMMENT '收货地址（JSON 格式）',
    remark       VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    create_time  DATETIME       NOT NULL,
    update_time  DATETIME       NOT NULL,
    deleted      TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id            BIGINT         NOT NULL COMMENT '主键',
    order_id      BIGINT         NOT NULL COMMENT '所属订单 ID',
    product_id    BIGINT         NOT NULL COMMENT '商品 ID',
    product_name  VARCHAR(200)   NOT NULL COMMENT '商品名称（快照）',
    product_image VARCHAR(255)   DEFAULT NULL COMMENT '商品图片（快照）',
    price         DECIMAL(10, 2) NOT NULL COMMENT '下单时单价（快照）',
    quantity      INT            NOT NULL COMMENT '购买数量',
    total_price   DECIMAL(10, 2) NOT NULL COMMENT '小计金额',
    create_time   DATETIME       NOT NULL,
    update_time   DATETIME       NOT NULL,
    deleted       TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';


-- ===== 物流数据库 =====
CREATE DATABASE IF NOT EXISTS mall_logistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall_logistics;

-- 物流单表
CREATE TABLE IF NOT EXISTS logistics (
    id               BIGINT      NOT NULL COMMENT '主键',
    order_id         BIGINT      NOT NULL COMMENT '关联订单 ID',
    order_no         VARCHAR(30) NOT NULL COMMENT '关联订单编号',
    company          VARCHAR(50) NOT NULL COMMENT '物流公司名称',
    tracking_no      VARCHAR(50) NOT NULL COMMENT '快递单号',
    status           TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0=待揽收，1=运输中，2=派送中，3=已签收，4=异常',
    receiver_name    VARCHAR(50) DEFAULT NULL COMMENT '收件人姓名',
    receiver_phone   VARCHAR(20) DEFAULT NULL COMMENT '收件人手机号',
    receiver_address TEXT        DEFAULT NULL COMMENT '收件地址',
    create_time      DATETIME    NOT NULL,
    update_time      DATETIME    NOT NULL,
    deleted          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tracking_no (tracking_no),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流单表';

-- 物流轨迹表
CREATE TABLE IF NOT EXISTS logistics_track (
    id           BIGINT       NOT NULL COMMENT '主键',
    logistics_id BIGINT       NOT NULL COMMENT '所属物流单 ID',
    location     VARCHAR(100) NOT NULL COMMENT '轨迹位置',
    remark       VARCHAR(500) NOT NULL COMMENT '轨迹描述',
    track_time   DATETIME     NOT NULL COMMENT '轨迹时间',
    track_type   TINYINT      NOT NULL DEFAULT 2 COMMENT '类型：1=揽收，2=转运，3=派送，4=签收，5=异常',
    create_time  DATETIME     NOT NULL,
    update_time  DATETIME     NOT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_logistics_id (logistics_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表';
