package com.example.spring_test.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductListVO {
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String categoryName;
    private String farmerName;
}