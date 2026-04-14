package com.example.spring_test.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String description;
    private String roleCode;
    private String avatar;
}
