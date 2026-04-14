package com.example.spring_test.service;

import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductCollocation;
import com.example.spring_test.entity.UserBehavior;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductCollocationMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.RecommendationLogMapper;
import com.example.spring_test.mapper.UserBehaviorMapper;
import com.example.spring_test.service.impl.RecommendationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RecommendationServiceTest {

    @Mock
    private UserBehaviorMapper userBehaviorMapper;

    @Mock
    private ProductCollocationMapper productCollocationMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductCategoryMapper productCategoryMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock ProductCategoryMapper - 返回默认分类
        ProductCategory defaultCategory = new ProductCategory();
        defaultCategory.setId(1L);
        defaultCategory.setCategoryName("蔬菜");
        when(productCategoryMapper.selectById(any())).thenReturn(defaultCategory);
    }

    @Test
    void testGetSmartRecommendations_WithViewHistory() {
        // 准备测试数据
        Long userId = 1L;
        List<Long> viewedProductIds = Arrays.asList(1L, 2L);
        
        // Mock 用户行为
        when(userBehaviorMapper.getViewedProductIdsByUserId(userId, 10)).thenReturn(viewedProductIds);
        
        // Mock 商品数据
        Product product1 = createProduct(1L, "测试商品1", new BigDecimal("19.9"));
        Product product2 = createProduct(2L, "测试商品2", new BigDecimal("29.9"));
        Product similarProduct = createProduct(3L, "相似商品", new BigDecimal("25.0"));
        
        when(productMapper.selectById(1L)).thenReturn(product1);
        when(productMapper.selectById(2L)).thenReturn(product2);
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(similarProduct));
        
        // 执行测试
        Map<String, Object> result = recommendationService.getSmartRecommendations(userId);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("reason"));
        assertTrue(result.containsKey("products"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        assertNotNull(products);
        
        // 验证缓存 - 第二次调用应该走缓存
        Map<String, Object> cachedResult = recommendationService.getSmartRecommendations(userId);
        assertNotNull(cachedResult);
        
        verify(userBehaviorMapper, times(1)).getViewedProductIdsByUserId(userId, 10);
    }

    @Test
    void testGetSmartRecommendations_WithoutViewHistory() {
        // 准备测试数据 - 没有浏览历史
        Long userId = 2L;
        
        when(userBehaviorMapper.getViewedProductIdsByUserId(userId, 10)).thenReturn(new ArrayList<>());
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
            createProduct(1L, "热门商品1", new BigDecimal("19.9")),
            createProduct(2L, "热门商品2", new BigDecimal("29.9"))
        ));
        
        // 执行测试
        Map<String, Object> result = recommendationService.getSmartRecommendations(userId);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("热门推荐", result.get("reason"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        assertFalse(products.isEmpty());
    }

    @Test
    void testGetCollocationRecommendations() {
        // 准备测试数据
        Long userId = 1L;
        List<Long> viewedProductIds = Arrays.asList(1L);
        
        when(userBehaviorMapper.getViewedProductIdsByUserId(userId, 5)).thenReturn(viewedProductIds);
        
        // Mock 搭配数据
        ProductCollocation collocation = new ProductCollocation();
        collocation.setId(1L);
        collocation.setProductId(1L);
        collocation.setCollocationProductId(2L);
        collocation.setScore(90);
        collocation.setDescription("牛肉炖萝卜");
        
        when(productCollocationMapper.getCollocationsByProductId(1L, 3))
            .thenReturn(Arrays.asList(collocation));
        
        Product collocationProduct = createProduct(2L, "白萝卜", new BigDecimal("2.5"));
        when(productMapper.selectById(2L)).thenReturn(collocationProduct);
        
        // 执行测试
        List<Map<String, Object>> result = recommendationService.getCollocationRecommendations(userId);
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        Map<String, Object> firstProduct = result.get(0);
        assertEquals("白萝卜", firstProduct.get("name"));
        assertEquals("牛肉炖萝卜", firstProduct.get("collocationDescription"));
    }

    @Test
    void testRecordUserBehavior() {
        // 准备测试数据
        Long userId = 1L;
        Long productId = 1L;
        Integer behaviorType = 1;
        Integer duration = 30;
        
        // 执行测试
        assertDoesNotThrow(() -> {
            recommendationService.recordUserBehavior(userId, productId, behaviorType, duration);
        });
        
        // 验证数据被保存
        verify(userBehaviorMapper, times(1)).insert(any(UserBehavior.class));
    }

    @Test
    void testCacheClearAfterBehavior() {
        // 准备测试数据
        Long userId = 1L;
        
        // 先获取推荐，建立缓存
        when(userBehaviorMapper.getViewedProductIdsByUserId(userId, 10))
            .thenReturn(Arrays.asList(1L));
        when(productMapper.selectById(any())).thenReturn(createProduct(1L, "测试", new BigDecimal("10.0")));
        when(productMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        recommendationService.getSmartRecommendations(userId);
        
        // 记录用户行为，应该清除缓存
        recommendationService.recordUserBehavior(userId, 1L, 1, 10);
        
        // 再次获取推荐，应该重新查询数据库
        recommendationService.getSmartRecommendations(userId);
        
        // 验证数据库被查询了两次（缓存被清除）
        verify(userBehaviorMapper, times(2)).getViewedProductIdsByUserId(userId, 10);
    }

    @Test
    void testGetCacheStats() {
        // 执行测试
        Map<String, Object> stats = recommendationService.getCacheStats();
        
        // 验证结果
        assertNotNull(stats);
        assertTrue(stats.containsKey("smartRecommendationCacheSize"));
        assertTrue(stats.containsKey("collocationCacheSize"));
        assertTrue(stats.containsKey("hotProductsCacheSize"));
    }

    private Product createProduct(Long id, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setProductName(name);
        product.setPrice(price);
        product.setUnitName("斤");
        product.setCoverImage("/test/image.jpg");
        product.setSaleStatus(1);
        product.setCategoryId(1L);
        return product;
    }
}
