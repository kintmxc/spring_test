package com.example.spring_test.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {
    private long pageNum = 1;
    private long pageSize = 10;
    private Long page;
    private String productName;
    private String keyword;
    private String originPlace;
    private Long categoryId;
    private Long farmerId;
    private Integer saleStatus;
    private Integer stockStatus;
    private Integer hasCoverImage;
    private String sort;
}