package com.example.spring_test.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}