package com.example.spring_test.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductSaveDTO {
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
}