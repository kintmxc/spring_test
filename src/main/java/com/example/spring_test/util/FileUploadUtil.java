package com.example.spring_test.util;

import com.example.spring_test.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public final class FileUploadUtil {
    public static final String ACCESS_PREFIX = "/uploads/";

    private FileUploadUtil() {
    }

    public static String save(MultipartFile file, String baseDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        try {
            Path uploadDir = Paths.get(baseDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String fileName = UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());
            return ACCESS_PREFIX + fileName;
        } catch (IOException exception) {
            throw new BusinessException("文件上传失败");
        }
    }
}