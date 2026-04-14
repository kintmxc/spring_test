package com.example.spring_test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private boolean enabled = true;
    private String provider = "zhipu";
    private String apiKey;
    private String model = "glm-4";
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
    private int timeout = 30000;
    private int maxTokens = 1024;
    private double temperature = 0.7;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
