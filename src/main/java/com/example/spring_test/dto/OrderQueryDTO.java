package com.example.spring_test.dto;

import lombok.Data;

@Data
public class OrderQueryDTO {
    private long pageNum = 1;
    private long pageSize = 10;
    private Long page;
    private String orderNo;
    private Long farmerId;
    private Long merchantId;
    private Integer orderStatus;
    private Integer status;
}