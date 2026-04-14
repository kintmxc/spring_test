package com.example.spring_test.ai;

import java.util.Map;

public interface AiContentService {
    String generateProductDescription(String productName, String category, String origin);
    
    String generateEatAdvice(String productName);
    
    String generateNutritionAnalysis(String productName);
    
    String generateStoreAdvice(String productName);
    
    Map<String, Object> generateAllContent(String productName, String category, String origin);
    
    String generateChatResponse(String content, java.util.List<java.util.Map<String, String>> history, String userType);
    
    String getProviderName();
    
    boolean isAvailable();
}
