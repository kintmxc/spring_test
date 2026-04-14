package com.example.spring_test.ai;

public interface AiProvider {
    String getName();
    
    boolean isAvailable();
    
    String chat(String prompt);
    
    String chatWithSystem(String systemPrompt, String userPrompt);
}
