INSERT IGNORE INTO admin_user (id, username, password, real_name, phone, role_code, status)
VALUES (1, 'admin', '123456', '系统管理员', '13800000000', 'ADMIN', 1);

INSERT IGNORE INTO farmer (id, login_name, password, farmer_name, contact_phone, origin_place, auth_status, account_status)
VALUES
(1, 'farmer01', '123456', '张家农场', '13900000001', 'XX县XX村', 1, 1),
(2, 'farmer02', '123456', '李家果园', '13900000002', 'XX县XX镇', 1, 1);

INSERT IGNORE INTO consumer_user (id, login_name, password, nick_name, phone, status)
VALUES
(1, 'consumer01', '123456', '普通用户01', '13700000001', 1);

INSERT IGNORE INTO product_category (id, category_name, sort_no, status)
VALUES
(1, '蔬菜', 1, 1),
(2, '水果', 2, 1),
(3, '粮油', 3, 1),
(4, '禽蛋', 4, 1),
(5, '特产', 5, 1);

INSERT IGNORE INTO product (id, farmer_id, category_id, product_name, price, stock, unit_name, origin_place, cover_image, description, sale_status, sales_count)
VALUES
(1, 1, 1, '本地白菜', 4.80, 120, '斤', 'XX县XX村', NULL, '新鲜采摘的本地白菜', 1, 16),
(2, 2, 2, '红富士苹果', 8.90, 80, '斤', 'XX县XX镇', NULL, '口感脆甜的红富士苹果', 1, 23);

INSERT IGNORE INTO product_trace (id, product_id, production_date, origin_desc, inspect_desc, trace_status)
VALUES
(1, 1, '2026-03-08', 'XX县XX村大棚种植', '抽样检测合格，无农残超标情况', 1),
(2, 2, '2026-03-07', 'XX县XX镇果园采摘', '外观检测正常，糖度达标', 1);

INSERT IGNORE INTO orders (id, order_no, user_id, farmer_id, total_amount, pay_amount, order_status, pay_status, receiver_name, receiver_phone, receiver_address, remark, pay_time, create_time, update_time)
VALUES
(1, 'ORD202603110001', 101, 1, 9.60, 9.60, 1, 1, '张三', '13600000001', 'XX市XX区XX路1号', '尽快发货', NOW(), NOW(), NOW()),
(2, 'ORD202603110002', 102, 2, 17.80, 17.80, 2, 1, '李四', '13600000002', 'XX市XX区XX路2号', '周末送达', NOW(), NOW(), NOW());

INSERT IGNORE INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal_amount)
VALUES
(1, 1, 1, '本地白菜', 4.80, 2, 9.60),
(2, 2, 2, '红富士苹果', 8.90, 2, 17.80);

INSERT IGNORE INTO order_logistics (id, order_id, company_name, tracking_no, logistics_status, ship_remark)
VALUES
(1, 2, '顺丰速运', 'SF202603110001', 1, '已从产地发出');

INSERT IGNORE INTO user_address (id, user_id, name, phone, province, city, district, detail, is_default)
VALUES
(1, 1, '管理员', '13800000000', '四川省', '成都市', '武侯区', '天府大道100号', 1);

INSERT IGNORE INTO evaluation (id, order_id, product_id, user_id, nick_name, avatar, score, content, images_json)
VALUES
(1, 1, 1, 1, '管**员', '', 5, '商品新鲜，口感不错', '[]');