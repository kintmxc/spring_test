-- ============================================
-- 修复 chat_message 表的 sender_user_id 字段
-- 执行时间: 2026-04-14
-- 说明: 为历史消息补全发送者用户ID
-- ============================================

-- 1. 先查看需要修复的数据量
SELECT '=== 修复前统计 ===' as step;
SELECT 
    COUNT(*) as total_messages,
    SUM(CASE WHEN sender_user_id IS NULL OR sender_user_id = 0 THEN 1 ELSE 0 END) as need_fix,
    SUM(CASE WHEN sender_user_id IS NOT NULL AND sender_user_id > 0 THEN 1 ELSE 0 END) as already_ok
FROM chat_message;

-- 2. 方案A：基于奇偶ID规则补全（适用于按时间顺序交替发送的场景）
-- 假设：奇数ID为消费者发的消息，偶数ID为农户发的消息

-- 2.1 补全消费者发的消息（奇数ID）
UPDATE chat_message 
SET sender_user_id = consumer_id
WHERE (sender_user_id IS NULL OR sender_user_id = 0)
  AND id % 2 = 1  -- 奇数ID
  AND consumer_id IS NOT NULL;

-- 2.2 补全农户发的消息（偶数ID）
UPDATE chat_message 
SET sender_user_id = farmer_id
WHERE (sender_user_id IS NULL OR sender_user_id = 0)
  AND id % 2 = 0  -- 偶数ID
  AND farmer_id IS NOT NULL;

-- 3. 查看修复结果
SELECT '=== 修复后统计 ===' as step;
SELECT 
    COUNT(*) as total_messages,
    SUM(CASE WHEN sender_user_id IS NULL OR sender_user_id = 0 THEN 1 ELSE 0 END) as still_null,
    SUM(CASE WHEN sender_user_id IS NOT NULL AND sender_user_id > 0 THEN 1 ELSE 0 END) as fixed,
    SUM(CASE WHEN sender_user_id = consumer_id THEN 1 ELSE 0 END) as consumer_messages,
    SUM(CASE WHEN sender_user_id = farmer_id THEN 1 ELSE 0 END) as farmer_messages
FROM chat_message;

-- 4. 查看修复后的数据示例
SELECT '=== 修复后数据示例 ===' as step;
SELECT 
    id, 
    session_id,
    farmer_id,
    consumer_id,
    sender_user_id,
    CASE 
        WHEN sender_user_id = consumer_id THEN '消费者'
        WHEN sender_user_id = farmer_id THEN '农户'
        ELSE '未知'
    END as sender_role,
    type,
    LEFT(content, 20) as content_preview,
    time
FROM chat_message 
ORDER BY session_id, id
LIMIT 20;

-- ============================================
-- 注意事项：
-- 1. 如果奇偶规则不适用，请使用方案B手动指定
-- 2. 修复后新消息会自动写入正确的 sender_user_id
-- 3. 建议在业务低峰期执行此脚本
-- ============================================
