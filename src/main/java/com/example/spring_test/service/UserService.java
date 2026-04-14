package com.example.spring_test.service;

import com.example.spring_test.dto.UserProfileUpdateDTO;
import com.example.spring_test.vo.UserProfileVO;

public interface UserService {
    UserProfileVO getProfile();
    UserProfileVO updateProfile(UserProfileUpdateDTO dto);
}
