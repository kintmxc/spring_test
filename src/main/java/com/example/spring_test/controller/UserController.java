package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.UserProfileUpdateDTO;
import com.example.spring_test.service.UserService;
import com.example.spring_test.vo.UserProfileVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        return Result.success("获取成功", userService.getProfile());
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        return Result.success("更新成功", userService.updateProfile(dto));
    }
}
