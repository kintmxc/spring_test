package com.example.spring_test.dto;

import lombok.Data;

@Data
public class OrderStatusUpdateDTO {
    private Integer targetStatus;
    private String remark;
}