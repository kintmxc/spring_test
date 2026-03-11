package com.example.spring_test.service;

import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);
}