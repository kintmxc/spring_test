package com.example.spring_test.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlUtils {
    private static String baseUrl;
    
    @Value("${app.upload.base-url:}")
    public void setBaseUrl(String baseUrl) {
        UrlUtils.baseUrl = baseUrl;
    }
    
    public static String toFullUrl(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            return path;
        }
        String base = baseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base + normalizedPath;
    }
}
