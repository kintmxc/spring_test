package com.example.spring_test.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    
    String upload(MultipartFile file);
    
    String upload(MultipartFile file, String subDir);
    
    void validateFile(MultipartFile file);
}
