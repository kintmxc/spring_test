-- 数据迁移脚本：将 product 表的 images 字段数据迁移到 product_image 表
-- 执行前请确保已创建 product_image 表

-- 1. 创建临时存储过程来解析JSON数组并插入数据
DELIMITER //

DROP PROCEDURE IF EXISTS migrate_product_images//

CREATE PROCEDURE migrate_product_images()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE p_id BIGINT;
    DECLARE p_images TEXT;
    DECLARE img_url VARCHAR(500);
    DECLARE img_index INT;
    DECLARE img_count INT;
    DECLARE json_array TEXT;
    
    -- 游标：查询所有有图片的商品
    DECLARE cur CURSOR FOR 
        SELECT id, images 
        FROM product 
        WHERE images IS NOT NULL 
          AND images != '' 
          AND images != '[]'
          AND images != 'null';
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO p_id, p_images;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 简单的JSON数组解析（假设格式为 ["url1","url2",...]）
        SET json_array = p_images;
        SET img_index = 0;
        SET img_count = 0;
        
        -- 计算图片数量（通过逗号数量估算）
        SET img_count = (LENGTH(json_array) - LENGTH(REPLACE(json_array, ',', ''))) + 1;
        
        -- 如果只有一个图片
        IF img_count = 1 THEN
            -- 提取单个URL
            SET img_url = TRIM(BOTH '"' FROM TRIM(BOTH '[' FROM TRIM(BOTH ']' FROM json_array)));
            IF img_url != '' AND img_url IS NOT NULL THEN
                INSERT INTO product_image (product_id, image_url, sort_order)
                VALUES (p_id, img_url, 0);
            END IF;
        ELSE
            -- 多个图片的情况，逐个提取
            WHILE img_index < img_count DO
                -- 简化的提取逻辑（实际项目中应使用更健壮的JSON解析）
                SET img_url = SUBSTRING_INDEX(
                    SUBSTRING_INDEX(
                        REPLACE(REPLACE(json_array, '[', ''), ']', ''),
                        ',', 
                        img_index + 1
                    ), 
                    ',', 
                    -1
                );
                
                SET img_url = TRIM(BOTH '"' FROM TRIM(img_url));
                
                IF img_url != '' AND img_url IS NOT NULL THEN
                    INSERT INTO product_image (product_id, image_url, sort_order)
                    VALUES (p_id, img_url, img_index);
                END IF;
                
                SET img_index = img_index + 1;
            END WHILE;
        END IF;
        
    END LOOP;
    
    CLOSE cur;
    
    SELECT CONCAT('迁移完成，共处理 ', COUNT(*), ' 条商品图片数据') AS result
    FROM product_image;
END//

DELIMITER ;

-- 2. 执行迁移存储过程
CALL migrate_product_images();

-- 3. 验证迁移结果
SELECT 
    p.id AS product_id,
    p.product_name,
    p.images AS old_images_json,
    GROUP_CONCAT(pi.image_url ORDER BY pi.sort_order SEPARATOR ', ') AS new_images
FROM product p
LEFT JOIN product_image pi ON p.id = pi.product_id
WHERE p.images IS NOT NULL AND p.images != '' AND p.images != '[]'
GROUP BY p.id, p.product_name, p.images
LIMIT 10;

-- 4. 清理存储过程
DROP PROCEDURE IF EXISTS migrate_product_images;

-- 5. 可选：确认迁移成功后，删除 product 表的 images 字段
-- ALTER TABLE product DROP COLUMN images;
