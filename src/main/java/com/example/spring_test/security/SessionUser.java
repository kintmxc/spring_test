package com.example.spring_test.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionUser {
    private Long userId;
    private String username;
    private String roleCode;
}