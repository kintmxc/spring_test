package com.example.spring_test.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ProductDetailVO {
    private Long id;
    private Long farmerId;
    private Long categoryId;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private String unitName;
    private String originPlace;
    private String coverImage;
    private List<String> images;
    private String description;
    private Integer saleStatus;
    private Integer salesCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String categoryName;
    private String farmerName;
    private String farmerDesc;
    private Long traceId;
    private boolean traceMaintained;
    private LocalDate productionDate;
    private String originDesc;
    private String inspectDesc;
    private Integer traceStatus;
    private String traceStatusText;
}