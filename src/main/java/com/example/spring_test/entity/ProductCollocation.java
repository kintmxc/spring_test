package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_collocation")
public class ProductCollocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long collocationProductId;
    private Integer score;
    private String description;
}
