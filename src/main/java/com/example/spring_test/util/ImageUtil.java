package com.example.spring_test.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUtil {
    
    @Value("${app.upload.default-image}")
    private String defaultImage;
    
    public String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return defaultImage;
        }
        
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        
        return imagePath;
    }
}
