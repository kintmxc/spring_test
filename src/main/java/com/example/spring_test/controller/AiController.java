package com.example.spring_test.controller;

import com.example.spring_test.ai.AiContentService;
import com.example.spring_test.common.Result;
import com.example.spring_test.dto.AiSalesAnalysisRequestDTO;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.AiSalesAnalysisService;
import com.example.spring_test.service.RecommendationService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiContentService aiContentService;
    private final RecommendationService recommendationService;
    private final AiSalesAnalysisService aiSalesAnalysisService;

    public AiController(AiContentService aiContentService,
                        RecommendationService recommendationService,
                        AiSalesAnalysisService aiSalesAnalysisService) {
        this.aiContentService = aiContentService;
        this.recommendationService = recommendationService;
        this.aiSalesAnalysisService = aiSalesAnalysisService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(Map.of(
                "enabled", aiContentService.isAvailable(),
                "provider", aiContentService.getProviderName()
        ));
    }

    @PostMapping("/generate/description")
    public Result<Map<String, Object>> generateDescription(@RequestBody Map<String, String> request) {
        String productName = request.get("productName");
        String category = request.get("category");
        String origin = request.get("origin");
        
        String description = aiContentService.generateProductDescription(productName, category, origin);
        return Result.success(Map.of(
                "content", description,
                "productName", productName,
                "aiGenerated", aiContentService.isAvailable(),
                "provider", aiContentService.getProviderName()
        ));
    }

    @PostMapping("/generate/eat-advice")
    public Result<Map<String, Object>> generateEatAdvice(@RequestBody Map<String, String> request) {
        String productName = request.get("productName");
        String advice = aiContentService.generateEatAdvice(productName);
        return Result.success(Map.of(
                "productName", productName,
                "advice", advice,
                "aiGenerated", aiContentService.isAvailable()
        ));
    }

    @PostMapping("/generate/nutrition")
    public Result<Map<String, Object>> generateNutrition(@RequestBody Map<String, String> request) {
        String productName = request.get("productName");
        String analysis = aiContentService.generateNutritionAnalysis(productName);
        return Result.success(Map.of(
                "productName", productName,
                "nutritionAnalysis", analysis,
                "aiGenerated", aiContentService.isAvailable()
        ));
    }

    @PostMapping("/generate/store-advice")
    public Result<Map<String, Object>> generateStoreAdvice(@RequestBody Map<String, String> request) {
        String productName = request.get("productName");
        String advice = aiContentService.generateStoreAdvice(productName);
        return Result.success(Map.of(
                "productName", productName,
                "storeAdvice", advice,
                "aiGenerated", aiContentService.isAvailable()
        ));
    }

    @PostMapping("/generate/all")
    public Result<Map<String, Object>> generateAll(@RequestBody Map<String, String> request) {
        String productName = request.get("productName");
        String category = request.get("category");
        String origin = request.get("origin");
        
        Map<String, Object> content = aiContentService.generateAllContent(productName, category, origin);
        return Result.success(content);
    }

    @GetMapping("/advice")
    public Result<Map<String, Object>> advice(@RequestParam String productName) {
        String eatAdvice = aiContentService.generateEatAdvice(productName);
        String storeAdvice = aiContentService.generateStoreAdvice(productName);
        return Result.success(Map.of(
                "eatAdvice", eatAdvice,
                "storeAdvice", storeAdvice
        ));
    }

    @PostMapping("/sales-analysis")
    public Result<Map<String, Object>> salesAnalysis(@RequestBody AiSalesAnalysisRequestDTO request) {
        if (request == null || request.getFarmerId() == null || !StringUtils.hasText(request.getDateType())) {
            return Result.fail("farmerId 和 dateType 不能为空");
        }
        String dateType = request.getDateType().toLowerCase(Locale.ROOT);
        if (!Set.of("week", "month", "year").contains(dateType)) {
            return Result.fail("dateType 仅支持 week/month/year");
        }
        Map<String, Object> data = aiSalesAnalysisService.analyze(request.getFarmerId(), dateType);
        return Result.success("AI分析成功", data);
    }
    
    @PostMapping("/chat")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        java.util.List<java.util.Map<String, String>> history = (java.util.List<java.util.Map<String, String>>) request.get("history");
        String type = (String) request.get("type");
        
        if (history == null) {
            history = new java.util.ArrayList<>();
        }
        if (type == null) {
            type = "consumer";
        }
        
        String response = aiContentService.generateChatResponse(content, history, type);
        return Result.success(Map.of(
                "content", response
        ));
    }
    
    @GetMapping("/recommendation")
    public Result<Map<String, Object>> getRecommendation() {
        Long userId = CurrentUserUtil.currentUserId();
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        
        Map<String, Object> recommendations = recommendationService.getSmartRecommendations(userId);
        return Result.success("获取智能推荐成功", recommendations);
    }
    
    @GetMapping("/collocation")
    public Result<List<Map<String, Object>>> getCollocation() {
        Long userId = CurrentUserUtil.currentUserId();
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        
        List<Map<String, Object>> collocations = recommendationService.getCollocationRecommendations(userId);
        return Result.success("获取搭配推荐成功", collocations);
    }
    
    @PostMapping("/behavior")
    public Result<Void> recordBehavior(@RequestBody Map<String, Object> request) {
        Long userId = CurrentUserUtil.currentUserId();
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        
        Long productId = ((Number) request.get("productId")).longValue();
        Integer behaviorType = ((Number) request.get("behaviorType")).intValue();
        Integer duration = request.get("duration") != null ? ((Number) request.get("duration")).intValue() : 0;
        
        recommendationService.recordUserBehavior(userId, productId, behaviorType, duration);
        return Result.success("记录用户行为成功", null);
    }
}
