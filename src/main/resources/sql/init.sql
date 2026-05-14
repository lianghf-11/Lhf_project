-- ============================================================
-- 秒杀系统数据库初始化脚本
-- 执行方式：MySQL 命令行 source init.sql 或 Navicat/IDEA 直接运行
-- ============================================================

-- 创建数据库（utf8mb4 支持 emoji 和中文）
CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARSET utf8mb4;
USE seckill;

-- ============================================================
-- 秒杀商品表 —— 核心表之一
-- price 用 DECIMAL 而非 FLOAT：浮点数计算会有精度问题，金额必须精确
-- ============================================================
CREATE TABLE IF NOT EXISTS seckill_goods (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL COMMENT '商品名称',
    description VARCHAR(500)  DEFAULT '' COMMENT '商品描述',
    price       DECIMAL(10,2) NOT NULL COMMENT '秒杀价格（元）',
    stock       INT           NOT NULL COMMENT '剩余库存数量',
    start_time  DATETIME      NOT NULL COMMENT '秒杀开始时间',
    end_time    DATETIME      NOT NULL COMMENT '秒杀结束时间'
) ENGINE=InnoDB COMMENT='秒杀商品';

-- ============================================================
-- 秒杀订单表 —— 核心表之二
-- goods_name 和 price 是快照字段：防止商品改名/改价后历史订单数据被污染
-- idx_user_goods 联合索引：快速判断用户是否重复购买同一商品
-- ============================================================
CREATE TABLE IF NOT EXISTS seckill_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    goods_id    BIGINT        NOT NULL COMMENT '商品ID',
    goods_name  VARCHAR(100)  DEFAULT '' COMMENT '商品名称快照（下单时的商品名）',
    price       DECIMAL(10,2) DEFAULT 0 COMMENT '价格快照（下单时的价格）',
    status      TINYINT       DEFAULT 1 COMMENT '订单状态：1=已下单 2=已支付 3=已取消',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_goods (user_id, goods_id)  -- 联合索引，防重复查询
) ENGINE=InnoDB COMMENT='秒杀订单';

-- ============================================================
-- 测试数据：5 个商品，时间窗口为当前~7天后，立即可测
-- 价格设为 0.01（1分钱）方便测试
-- ============================================================
INSERT INTO seckill_goods (name, description, price, stock, start_time, end_time)
VALUES
('iPhone 16 Pro Max',   '最新款苹果手机，限时秒杀',    0.01, 100,  NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('MacBook Pro 16"',     'M4芯片笔记本，超值秒杀',     0.01, 50,   NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('AirPods Pro 3',       '降噪耳机，秒杀专场',        0.01, 200,  NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('iPad Air M4',         '平板电脑，限时特惠',        0.01, 80,   NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY)),
('Apple Watch Ultra 3', '运动手表，秒杀价',          0.01, 150,  NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY));
