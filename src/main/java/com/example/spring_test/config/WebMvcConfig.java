package com.example.spring_test.config;

import com.example.spring_test.security.LoginInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    private final LoginInterceptor loginInterceptor;
    
    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
        log.info("=== WebMvcConfig initialized with LoginInterceptor ===");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("=== Registering LoginInterceptor ===");
        log.info("Excluded paths: /api/auth/login, /api/auth/logout, /api/auth/sms, /api/auth/register, /api/auth/phone-login, /api/auth/wechat-login, /api/ai/advice, /api/ai/chat, /api/chat/**, /api/categories, /api/categories/options, /api/sales-rank, /sayHello, /error");
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/login", "/api/auth/logout", "/api/auth/sms", "/api/auth/register", "/api/auth/phone-login", "/api/auth/wechat-login", "/api/ai/advice", "/api/ai/chat", "/api/chat/**", "/api/categories", "/api/categories/options", "/api/sales-rank", "/sayHello", "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadLocation = uploadPath.toUri().toString();
        log.info("=== Static resource mapping: /uploads/** -> {}", uploadLocation);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}