package com.example.spring_test.dto;

import lombok.Data;

@Data
public class ProductStockUpdateDTO {
    private Integer stock;
    private String adjustReason;
}