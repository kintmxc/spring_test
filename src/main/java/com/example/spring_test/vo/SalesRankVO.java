package com.example.spring_test.vo;

import lombok.Data;

@Data
public class SalesRankVO {
    private Long id;
    private String name;
    private java.math.BigDecimal price;
    private String unit;
    private String image;
    private String farmerName;
    private Integer salesVolume;
    private java.math.BigDecimal salesAmount;
}
