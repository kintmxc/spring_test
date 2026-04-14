package com.example.spring_test.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private String token;
    private LoginUserInfoVO userInfo;

    public LoginVO(Long userId, String username, String realName, String roleCode) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleCode = roleCode;
    }
}