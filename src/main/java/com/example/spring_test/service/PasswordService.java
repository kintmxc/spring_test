package com.example.spring_test.service;

public interface PasswordService {
    
    String encode(String rawPassword);
    
    boolean matches(String rawPassword, String encodedPassword);
    
    boolean needsUpgrade(String encodedPassword);
    
    void validatePassword(String password);
}
