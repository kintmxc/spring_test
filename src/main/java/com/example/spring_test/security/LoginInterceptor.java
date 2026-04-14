package com.example.spring_test.security;

import com.example.spring_test.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);
    public static final String SESSION_KEY = "LOGIN_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info("=== LoginInterceptor Debug ===");
        log.info("Request Path: {}", path);
        log.info("Request Method: {}", method);
        log.info("isPublicReadEndpoint: {}", isPublicReadEndpoint(request));
        
        if (isPublicReadEndpoint(request)) {
            log.info(">>> Public endpoint, allowing access");
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.info(">>> No session found, throwing UnauthorizedException");
            throw new UnauthorizedException("未登录或登录已过期");
        }
        Object sessionUser = session.getAttribute(SESSION_KEY);
        if (sessionUser instanceof SessionUser user) {
            SessionUserHolder.set(user);
            log.info(">>> Valid session found, allowing access");
            return true;
        }
        log.info(">>> Invalid session, throwing UnauthorizedException");
        throw new UnauthorizedException("未登录或登录已过期");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SessionUserHolder.clear();
    }

    private boolean isPublicReadEndpoint(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/api/products") || path.equals("/api/categories") || path.equals("/api/categories/options"));
    }
}