package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long farmerId;
    private Long categoryId;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private String unitName;
    private String originPlace;
    private String coverImage;
    private String description;
    private Integer saleStatus;
    private Integer salesCount;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}