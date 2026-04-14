package com.example.spring_test.vo;

import lombok.Data;

@Data
public class LoginUserInfoVO {
    private Long id;
    private String phone;
    private String nickName;
    private String role;
    private String avatar;
    private String address;
    private String username;
    private String realName;
    private String roleCode;
}
