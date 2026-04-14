package com.example.spring_test.service.impl;

import com.example.spring_test.config.SecurityProperties;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.service.FileUploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    
    public static final String ACCESS_PREFIX = "/uploads/";
    
    @Value("${app.upload.dir}")
    private String baseDir;
    
    private final SecurityProperties securityProperties;
    
    public FileUploadServiceImpl(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public String upload(MultipartFile file) {
        return upload(file, null);
    }

    @Override
    public String upload(MultipartFile file, String subDir) {
        validateFile(file);
        
        try {
            String uploadPath = subDir != null ? baseDir + "/" + subDir : baseDir;
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            
            String fileName = UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());
            
            return ACCESS_PREFIX + (subDir != null ? subDir + "/" : "") + fileName;
        } catch (IOException exception) {
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        
        SecurityProperties.FileUpload config = securityProperties.getFileUpload();
        
        if (file.getSize() > config.getMaxSizeBytes()) {
            throw new BusinessException("文件大小超过限制，最大允许 " + (config.getMaxSizeBytes() / 1024 / 1024) + "MB");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }
        
        final String extension;
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        } else {
            extension = "";
        }
        
        if (config.getAllowedExtensions() != null && config.getAllowedExtensions().length > 0) {
            boolean extensionAllowed = Arrays.stream(config.getAllowedExtensions())
                    .anyMatch(ext -> ext.equalsIgnoreCase(extension));
            if (!extensionAllowed) {
                throw new BusinessException("不支持的文件类型，仅支持: " + String.join(", ", config.getAllowedExtensions()));
            }
        }
        
        if (config.isValidateMimeType() && config.getAllowedMimeTypes() != null) {
            String contentType = file.getContentType();
            String[] allowedTypes = config.getAllowedMimeTypes();
            if (contentType == null || Arrays.stream(allowedTypes).noneMatch(contentType::equals)) {
                throw new BusinessException("不支持的文件 MIME 类型");
            }
        }
    }
}
