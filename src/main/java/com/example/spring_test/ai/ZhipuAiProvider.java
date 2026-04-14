package com.example.spring_test.ai;

import com.example.spring_test.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ZhipuAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(ZhipuAiProvider.class);
    
    private final AiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ZhipuAiProvider(AiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "zhipu";
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    @Override
    public String chat(String prompt) {
        return chatWithSystem(null, prompt);
    }

    @Override
    public String chatWithSystem(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
        return "AI服务未配置，请检查API Key设置";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "messages", messages,
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                    config.getBaseUrl() + "/chat/completions",
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content != null) {
                    return content.asText();
                }
            }
            
            return "AI生成内容失败，请稍后重试";
        } catch (Exception e) {
            log.error("智谱AI调用失败: {}", e.getMessage());
            return "AI服务暂时不可用: " + e.getMessage();
        }
    }
}
