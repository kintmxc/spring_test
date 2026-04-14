-- AI 推荐功能数据库表初始化
-- 创建时间: 2026-04-12

-- 用户行为表
CREATE TABLE IF NOT EXISTS user_behavior (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    behavior_type TINYINT NOT NULL COMMENT '行为类型（1-浏览，2-购买，3-加入购物车，4-收藏）',
    behavior_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    duration INT DEFAULT 0 COMMENT '浏览时长（秒）',
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_behavior_time (behavior_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- 商品搭配表
CREATE TABLE IF NOT EXISTS product_collocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    collocation_product_id BIGINT NOT NULL COMMENT '搭配商品ID',
    score INT NOT NULL DEFAULT 0 COMMENT '搭配分数（0-100）',
    description VARCHAR(255) COMMENT '搭配描述',
    INDEX idx_product_id (product_id),
    INDEX idx_collocation_product_id (collocation_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品搭配表';

-- 推荐日志表
CREATE TABLE IF NOT EXISTS recommendation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    recommendation_type TINYINT NOT NULL COMMENT '推荐类型（1-智能推荐，2-搭配推荐）',
    recommendation_data JSON COMMENT '推荐数据（JSON格式）',
    recommendation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    click_count INT DEFAULT 0 COMMENT '点击次数',
    purchase_count INT DEFAULT 0 COMMENT '购买次数',
    INDEX idx_user_id (user_id),
    INDEX idx_recommendation_time (recommendation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐日志表';

-- 插入示例搭配数据（可选）
-- INSERT INTO product_collocation (product_id, collocation_product_id, score, description) VALUES
-- (1, 2, 90, '牛肉炖萝卜'),
-- (1, 3, 85, '鸡肉配土豆');
