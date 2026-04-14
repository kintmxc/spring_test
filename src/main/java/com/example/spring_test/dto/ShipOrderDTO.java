package com.example.spring_test.dto;

import lombok.Data;

@Data
public class ShipOrderDTO {
    private String companyName;
    private String logisticsCompany;
    private String trackingNo;
    private String shipRemark;
}