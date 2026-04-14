package com.example.spring_test.service.impl;

import com.example.spring_test.config.SecurityProperties;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.service.PasswordService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordServiceImpl implements PasswordService {
    
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    
    private final SecurityProperties securityProperties;
    
    public PasswordServiceImpl(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (isBcrypt(encodedPassword)) {
            return ENCODER.matches(rawPassword, encodedPassword);
        }
        if (securityProperties.getPasswordValidation().isRequireBcryptOnly()) {
            return false;
        }
        return rawPassword.equals(encodedPassword);
    }

    @Override
    public boolean needsUpgrade(String encodedPassword) {
        return encodedPassword != null && !isBcrypt(encodedPassword);
    }

    @Override
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        SecurityProperties.PasswordValidation config = securityProperties.getPasswordValidation();
        if (password.length() < config.getMinLength()) {
            throw new BusinessException("密码长度不能少于" + config.getMinLength() + "位");
        }
        if (config.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含大写字母");
        }
        if (config.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含小写字母");
        }
        if (config.isRequireDigit() && !password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }
        if (config.isRequireSpecialChar() && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new BusinessException("密码必须包含特殊字符");
        }
    }
    
    private boolean isBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
