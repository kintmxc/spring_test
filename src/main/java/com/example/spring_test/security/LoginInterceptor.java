package com.example.spring_test.security;

import com.example.spring_test.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {
    public static final String SESSION_KEY = "LOGIN_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object sessionUser = request.getSession().getAttribute(SESSION_KEY);
        if (sessionUser instanceof SessionUser user) {
            SessionUserHolder.set(user);
            return true;
        }
        throw new UnauthorizedException("未登录或登录已过期");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SessionUserHolder.clear();
    }
}