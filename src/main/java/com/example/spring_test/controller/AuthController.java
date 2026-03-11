package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.security.LoginInterceptor;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.AuthService;
import com.example.spring_test.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        LoginVO loginVO = authService.login(loginDTO);
        request.getSession().setAttribute(LoginInterceptor.SESSION_KEY,
                new SessionUser(loginVO.getUserId(), loginVO.getUsername(), loginVO.getRoleCode()));
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return Result.success("退出成功", null);
    }

    @GetMapping("/me")
    public Result<SessionUser> currentUser() {
        return Result.success(SessionUserHolder.get());
    }
}