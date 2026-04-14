package com.example.spring_test.service;

import java.util.List;
import java.util.Map;

public interface RecommendationService {
    
    /**
     * 获取智能推荐商品
     */
    Map<String, Object> getSmartRecommendations(Long userId);
    
    /**
     * 获取搭配推荐商品
     */
    List<Map<String, Object>> getCollocationRecommendations(Long userId);
    
    /**
     * 记录用户行为
     */
    void recordUserBehavior(Long userId, Long productId, Integer behaviorType, Integer duration);
}
