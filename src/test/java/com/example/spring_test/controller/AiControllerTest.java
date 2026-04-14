package com.example.spring_test.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.spring_test.ai.AiContentService;
import com.example.spring_test.common.Result;
import com.example.spring_test.service.AiSalesAnalysisService;
import com.example.spring_test.service.RecommendationService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiContentService aiContentService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private AiSalesAnalysisService aiSalesAnalysisService;

    private AiController aiController;

    @BeforeEach
    void setUp() {
        aiController = new AiController(aiContentService, recommendationService, aiSalesAnalysisService);
    }

    @Test
    void generateDescription_shouldReturnContentFieldForFrontendContract() {
        when(aiContentService.generateProductDescription("散养土鸡", "禽类", "四川")).thenReturn("散养土鸡，肉质紧实鲜香，适合家庭炖煮与清炒。");
        when(aiContentService.isAvailable()).thenReturn(true);
        when(aiContentService.getProviderName()).thenReturn("zhipu");

        Result<Map<String, Object>> result = aiController.generateDescription(Map.of(
                "productName", "散养土鸡",
                "category", "禽类",
                "origin", "四川"
        ));

        assertTrue(result.isSuccess());
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("散养土鸡，肉质紧实鲜香，适合家庭炖煮与清炒。", result.getData().get("content"));
    }
}
