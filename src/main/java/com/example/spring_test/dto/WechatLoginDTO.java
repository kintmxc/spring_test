package com.example.spring_test.dto;

import lombok.Data;

@Data
public class WechatLoginDTO {
    private String code;
    private String nickName;
    private String avatar;
}
