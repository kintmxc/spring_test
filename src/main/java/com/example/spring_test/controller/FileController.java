package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.service.FileService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FileController {
    private final FileService fileService;
    
    @Value("${app.upload.base-url:}")
    private String baseUrl;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/files/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String path = fileService.uploadImage(file);
        String fullUrl = buildFullUrl(path);
        return Result.success(Map.of("url", fullUrl, "path", path, "fullUrl", fullUrl));
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return uploadImage(file);
    }
    
    private String buildFullUrl(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = baseUrl;
        if (base == null || base.isEmpty()) {
            return path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base + normalizedPath;
    }
}
