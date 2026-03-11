package com.example.spring_test.dto;

import lombok.Data;

@Data
public class FarmerQueryDTO {
    private long pageNum = 1;
    private long pageSize = 10;
    private String farmerName;
    private Integer authStatus;
    private Integer accountStatus;
}