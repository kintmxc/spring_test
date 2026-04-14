-- 优化后的数据库表结构

-- 管理员表
CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    role_code VARCHAR(32) NOT NULL DEFAULT 'ADMIN' COMMENT '角色编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_admin_user_username (username)
) COMMENT='管理员表';

-- 农户表
CREATE TABLE IF NOT EXISTS farmer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    login_name VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码',
    farmer_name VARCHAR(64) NOT NULL COMMENT '农户名称',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    origin_place VARCHAR(128) DEFAULT NULL COMMENT '产地说明',
    description TEXT DEFAULT NULL COMMENT '农户介绍',
    id_card_no VARCHAR(32) DEFAULT NULL COMMENT '身份证号',
    license_no VARCHAR(64) DEFAULT NULL COMMENT '资质编号',
    auth_status TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态 0待审核 1已通过 2已驳回',
    account_status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 1启用 0禁用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_farmer_login_name (login_name),
    KEY idx_farmer_auth_status (auth_status),
    KEY idx_farmer_account_status (account_status)
) COMMENT='农户表';

-- 消费者表
CREATE TABLE IF NOT EXISTS consumer_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    login_name VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码',
    nick_name VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    description TEXT DEFAULT NULL COMMENT '用户介绍',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_consumer_user_login_name (login_name),
    KEY idx_consumer_user_status (status)
) COMMENT='消费者表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_category_name (category_name)
) COMMENT='商品分类表';

-- 农产品表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    farmer_id BIGINT NOT NULL COMMENT '所属农户ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    unit_name VARCHAR(32) DEFAULT NULL COMMENT '单位',
    origin_place VARCHAR(128) DEFAULT NULL COMMENT '产地',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图路径',
    description TEXT DEFAULT NULL COMMENT '商品描述',
    sale_status TINYINT NOT NULL DEFAULT 1 COMMENT '上下架状态 1上架 0下架',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '销量',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product_farmer_id (farmer_id),
    KEY idx_product_category_id (category_id),
    KEY idx_product_sale_status (sale_status)
) COMMENT='农产品表';

-- 商品图片表
CREATE TABLE IF NOT EXISTS product_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_product_image_product_id (product_id),
    CONSTRAINT fk_product_image_product_id FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
) COMMENT='商品图片表';

-- 商品追溯表
CREATE TABLE IF NOT EXISTS product_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    origin_desc VARCHAR(255) DEFAULT NULL COMMENT '产地说明',
    inspect_desc VARCHAR(255) DEFAULT NULL COMMENT '检测说明',
    trace_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1有效 0停用',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_trace_product_id (product_id)
) COMMENT='商品追溯表';

-- 订单主表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '消费者ID',
    farmer_id BIGINT NOT NULL COMMENT '农户ID',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态 0待支付 1已支付 2已发货 3已完成 4已取消',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2已退款',
    receiver_name VARCHAR(64) DEFAULT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20) DEFAULT NULL COMMENT '收货电话',
    receiver_address VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
    remark VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    ship_time DATETIME DEFAULT NULL COMMENT '发货时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_user_id (user_id),
    KEY idx_orders_farmer_id (farmer_id),
    KEY idx_orders_status (order_status)
) COMMENT='订单主表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称快照',
    product_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品单价快照',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    subtotal_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小计金额',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_order_item_order_id (order_id),
    KEY idx_order_item_product_id (product_id)
) COMMENT='订单明细表';

-- 订单物流表
CREATE TABLE IF NOT EXISTS order_logistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    company_name VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
    tracking_no VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
    logistics_status TINYINT NOT NULL DEFAULT 0 COMMENT '物流状态 0待发货 1已发货 2已签收',
    ship_remark VARCHAR(255) DEFAULT NULL COMMENT '发货备注',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_logistics_order_id (order_id),
    KEY idx_order_logistics_tracking_no (tracking_no)
) COMMENT='订单物流表';

-- 用户地址表
CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(64) NOT NULL COMMENT '收货人',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    province VARCHAR(64) DEFAULT NULL COMMENT '省',
    city VARCHAR(64) DEFAULT NULL COMMENT '市',
    district VARCHAR(64) DEFAULT NULL COMMENT '区县',
    detail VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址 1是 0否',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_address_user_id (user_id),
    KEY idx_user_address_default (is_default)
) COMMENT='用户地址表';

-- 商品评价表
CREATE TABLE IF NOT EXISTS evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nick_name VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    score TINYINT NOT NULL COMMENT '评分 1-5',
    content VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    images_json TEXT DEFAULT NULL COMMENT '评价图片JSON',
    tags_json TEXT DEFAULT NULL COMMENT '评价标签JSON',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_evaluation_product_id (product_id),
    KEY idx_evaluation_user_id (user_id),
    KEY idx_evaluation_order_id (order_id)
) COMMENT='商品评价表';

-- 聊天会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    farmer_id BIGINT NOT NULL COMMENT '农户ID',
    farmer_name VARCHAR(100) DEFAULT NULL COMMENT '农户名称',
    consumer_id BIGINT NOT NULL COMMENT '消费者ID',
    consumer_name VARCHAR(100) DEFAULT NULL COMMENT '消费者名称',
    last_message TEXT DEFAULT NULL COMMENT '最后一条消息',
    last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_chat_session_farmer_consumer (farmer_id, consumer_id),
    KEY idx_chat_session_consumer_farmer (consumer_id, farmer_id)
) COMMENT='聊天会话表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    farmer_id BIGINT NOT NULL COMMENT '农户ID',
    consumer_id BIGINT NOT NULL COMMENT '消费者ID',
    type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型',
    content TEXT NOT NULL COMMENT '消息内容',
    time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '发送者头像',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    sender_type VARCHAR(20) DEFAULT 'consumer' COMMENT '发送者类型',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1删除 0正常',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_chat_message_session_time (session_id, time),
    KEY idx_chat_message_farmer_consumer (farmer_id, consumer_id),
    CONSTRAINT fk_chat_message_session_id FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
) COMMENT='聊天消息表';
