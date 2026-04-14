package com.example.spring_test.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UploadConfig {
    private static final Logger log = LoggerFactory.getLogger(UploadConfig.class);

    public UploadConfig(@Value("${app.upload.dir}") String uploadDir) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("=== Upload directory: {} ===", uploadPath);
        
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                log.info("=== Created upload directory: {} ===", uploadPath);
            } catch (IOException e) {
                log.error("=== Failed to create upload directory: {} ===", uploadPath, e);
            }
        } else {
            log.info("=== Upload directory already exists: {} ===", uploadPath);
        }
        
        if (Files.isWritable(uploadPath)) {
            log.info("=== Upload directory is writable ===");
        } else {
            log.warn("=== Upload directory is NOT writable! ===");
        }
    }
}
