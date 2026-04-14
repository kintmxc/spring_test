package com.example.spring_test.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private String phone;
    private String code;
}