package com.example.spring_test.service;

import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.vo.LoginVO;
import java.util.Map;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);

    LoginVO phoneLogin(String phone, String code, String role);

    LoginVO wechatQuickLogin(String wechatCode, String nickName);

    void registerByPhone(String phone, String code, String nickName, String role);

    Map<String, Object> getUserProfile(Long userId, String roleCode);
}