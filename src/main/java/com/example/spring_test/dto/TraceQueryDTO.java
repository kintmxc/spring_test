package com.example.spring_test.dto;

import lombok.Data;

@Data
public class TraceQueryDTO {
    private long pageNum = 1;
    private long pageSize = 10;
    private String productName;
    private Long farmerId;
    private Integer traceStatus;
}