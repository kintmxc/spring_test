package com.example.spring_test.dto;

import lombok.Data;

@Data
public class PhoneLoginDTO {
    private String phone;
    private String code;
    private String role;
}
