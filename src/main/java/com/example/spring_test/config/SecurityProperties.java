package com.example.spring_test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    
    private boolean strictMode = false;
    
    private SmsVerification smsVerification = new SmsVerification();
    
    private PasswordValidation passwordValidation = new PasswordValidation();
    
    private FileUpload fileUpload = new FileUpload();

    @Data
    public static class SmsVerification {
        private boolean useRealSms = false;
        private String mockCode = "123456";
        private String smsProviderUrl;
        private String smsApiKey;
    }

    @Data
    public static class PasswordValidation {
        private boolean requireBcryptOnly = false;
        private int minLength = 6;
        private boolean requireUppercase = false;
        private boolean requireLowercase = false;
        private boolean requireDigit = false;
        private boolean requireSpecialChar = false;
    }

    @Data
    public static class FileUpload {
        private long maxSizeBytes = 10 * 1024 * 1024;
        private String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx"};
        private String[] allowedMimeTypes = {"image/jpeg", "image/png", "image/gif", "application/pdf"};
        private boolean validateMimeType = false;
    }
}
