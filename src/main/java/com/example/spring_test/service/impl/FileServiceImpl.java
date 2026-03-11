package com.example.spring_test.service.impl;

import com.example.spring_test.service.FileService;
import com.example.spring_test.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {
    private final String uploadDir;

    public FileServiceImpl(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return FileUploadUtil.save(file, uploadDir);
    }
}