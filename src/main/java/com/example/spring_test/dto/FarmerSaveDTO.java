package com.example.spring_test.dto;

import lombok.Data;

@Data
public class FarmerSaveDTO {
    private String loginName;
    private String password;
    private String farmerName;
    private String contactPhone;
    private String originPlace;
    private String idCardNo;
    private String licenseNo;
    private String remark;
}