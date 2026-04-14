package com.example.spring_test.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spring_test.ai.AiContentService;
import com.example.spring_test.mapper.ChatMessageMapper;
import com.example.spring_test.mapper.ChatSessionMapper;
import com.example.spring_test.service.impl.ChatServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private AiContentService aiContentService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void generateAIResponse_shouldUseProductIntroPrompt_whenFarmerRequestsIntro() {
        String content = "请为商品\"散养土山鸡\"生成一段50字左右的商品介绍，突出特点和优势";
        when(aiContentService.generateChatResponse(org.mockito.ArgumentMatchers.contains("农产品电商文案助手"), anyList(), eq("farmer")))
                .thenReturn("散养土山鸡，野外放养，肉质紧实鲜美，口感更佳，适合家庭健康餐桌。")
                .thenReturn("散养土山鸡，野外放养，肉质紧实鲜美，口感更佳，适合家庭健康餐桌。");

        String result = chatService.generateAIResponse(content, List.of(), "farmer");

        assertTrue(result.contains("散养土山鸡"));
        assertTrue(result.length() <= 71);
        verify(aiContentService, times(1)).generateChatResponse(org.mockito.ArgumentMatchers.contains("农产品电商文案助手"), anyList(), eq("farmer"));
    }

    @Test
    void generateAIResponse_shouldKeepNormalChatPath_whenNotFarmerIntro() {
        String content = "今天天气怎么样";
        when(aiContentService.generateChatResponse(eq(content), org.mockito.ArgumentMatchers.anyList(), eq("consumer")))
                .thenReturn("今天天气晴朗。");

        String result = chatService.generateAIResponse(content, List.of(Map.of("role", "user", "content", "hello")), "consumer");

        assertTrue(result.contains("晴朗"));
        verify(aiContentService, times(1)).generateChatResponse(eq(content), org.mockito.ArgumentMatchers.anyList(), eq("consumer"));
    }
}
