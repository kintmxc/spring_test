package com.example.spring_test.service.impl;

import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductCollocation;
import com.example.spring_test.entity.UserBehavior;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductCollocationMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.RecommendationLogMapper;
import com.example.spring_test.mapper.UserBehaviorMapper;
import com.example.spring_test.service.RecommendationService;
import com.example.spring_test.util.UrlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private ProductCollocationMapper productCollocationMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Resource
    private RecommendationLogMapper recommendationLogMapper;

    @Resource
    private ObjectMapper objectMapper;

    // 本地缓存 - 智能推荐（5分钟过期，最多200个用户）
    private final Cache<Long, Map<String, Object>> smartRecommendationCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(200)
            .recordStats()
            .build();

    // 本地缓存 - 搭配推荐（10分钟过期，最多200个用户）
    private final Cache<Long, List<Map<String, Object>>> collocationCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(200)
            .recordStats()
            .build();

    // 本地缓存 - 热门商品（30分钟过期，只存一份）
    private final Cache<String, List<Product>> hotProductsCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1)
            .recordStats()
            .build();

    @Override
    public Map<String, Object> getSmartRecommendations(Long userId) {
        // 1. 尝试从本地缓存获取
        Map<String, Object> cachedResult = smartRecommendationCache.getIfPresent(userId);
        if (cachedResult != null) {
            log.info("从本地缓存获取智能推荐 - userId: {}", userId);
            return cachedResult;
        }

        log.info("从数据库获取智能推荐 - userId: {}", userId);

        List<Map<String, Object>> recommendedProducts = new ArrayList<>();
        String reason = "热门推荐";

        // 2. 基于用户浏览历史的协同过滤推荐
        List<Long> viewedProductIds = userBehaviorMapper.getViewedProductIdsByUserId(userId, 10);

        if (!viewedProductIds.isEmpty()) {
            reason = "根据您的浏览历史";
            // 获取相似商品（简化实现：基于分类和农户）
            List<Product> similarProducts = getSimilarProducts(viewedProductIds);
            recommendedProducts = convertToProductList(similarProducts);
        }

        // 3. 如果没有足够的推荐，补充热门商品
        if (recommendedProducts.size() < 5) {
            List<Product> hotProducts = getHotProductsFromCache(10);
            recommendedProducts.addAll(convertToProductList(hotProducts));
            reason = "热门推荐";
        }

        // 4. 去重并限制数量
        recommendedProducts = recommendedProducts.stream()
                .distinct()
                .limit(6)
                .collect(java.util.stream.Collectors.toList());

        // 5. 记录推荐日志
        try {
            String recommendationData = objectMapper.writeValueAsString(recommendedProducts);
            recommendationLogMapper.insertLog(userId, 1, recommendationData);
        } catch (Exception e) {
            // 记录日志失败不影响主流程
            log.warn("记录推荐日志失败", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reason", reason);
        result.put("products", recommendedProducts);

        // 6. 写入本地缓存
        smartRecommendationCache.put(userId, result);
        log.info("智能推荐结果已缓存到本地 - userId: {}, 缓存大小: {}", userId, smartRecommendationCache.estimatedSize());

        return result;
    }

    @Override
    public List<Map<String, Object>> getCollocationRecommendations(Long userId) {
        // 1. 尝试从本地缓存获取
        List<Map<String, Object>> cachedResult = collocationCache.getIfPresent(userId);
        if (cachedResult != null) {
            log.info("从本地缓存获取搭配推荐 - userId: {}", userId);
            return cachedResult;
        }

        log.info("从数据库获取搭配推荐 - userId: {}", userId);

        List<Map<String, Object>> collocationProducts = new ArrayList<>();

        // 2. 获取用户最近浏览的商品
        List<Long> viewedProductIds = userBehaviorMapper.getViewedProductIdsByUserId(userId, 5);

        if (!viewedProductIds.isEmpty()) {
            // 3. 基于最近浏览的商品获取搭配推荐
            for (Long productId : viewedProductIds) {
                List<ProductCollocation> collocations = productCollocationMapper.getCollocationsByProductId(productId, 3);
                for (ProductCollocation collocation : collocations) {
                    Product product = productMapper.selectById(collocation.getCollocationProductId());
                    if (product != null) {
                        Map<String, Object> productMap = convertToProductMap(product);
                        productMap.put("collocationDescription", collocation.getDescription());
                        collocationProducts.add(productMap);
                    }
                }
            }
        }

        // 4. 如果没有足够的搭配推荐，获取高分搭配
        if (collocationProducts.size() < 5) {
            List<ProductCollocation> topCollocations = productCollocationMapper.getTopCollocations(10);
            for (ProductCollocation collocation : topCollocations) {
                Product product = productMapper.selectById(collocation.getCollocationProductId());
                if (product != null) {
                    Map<String, Object> productMap = convertToProductMap(product);
                    productMap.put("collocationDescription", collocation.getDescription());
                    collocationProducts.add(productMap);
                }
            }
        }

        // 5. 去重并限制数量
        collocationProducts = collocationProducts.stream()
                .distinct()
                .limit(6)
                .collect(java.util.stream.Collectors.toList());

        // 6. 记录推荐日志
        try {
            String recommendationData = objectMapper.writeValueAsString(collocationProducts);
            recommendationLogMapper.insertLog(userId, 2, recommendationData);
        } catch (Exception e) {
            // 记录日志失败不影响主流程
            log.warn("记录搭配推荐日志失败", e);
        }

        // 7. 写入本地缓存
        collocationCache.put(userId, collocationProducts);
        log.info("搭配推荐结果已缓存到本地 - userId: {}, 缓存大小: {}", userId, collocationCache.estimatedSize());

        return collocationProducts;
    }

    @Override
    public void recordUserBehavior(Long userId, Long productId, Integer behaviorType, Integer duration) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setProductId(productId);
        behavior.setBehaviorType(behaviorType);
        behavior.setBehaviorTime(LocalDateTime.now());
        behavior.setDuration(duration);
        userBehaviorMapper.insert(behavior);

        log.info("记录用户行为 - userId: {}, productId: {}, behaviorType: {}", userId, productId, behaviorType);

        // 清除用户相关的推荐缓存，使下次推荐能基于最新行为
        clearUserRecommendationCache(userId);
    }

    /**
     * 从本地缓存获取热门商品
     */
    private List<Product> getHotProductsFromCache(Integer limit) {
        List<Product> cachedProducts = hotProductsCache.getIfPresent("hot");
        if (cachedProducts != null) {
            log.info("从本地缓存获取热门商品");
            return cachedProducts.stream().limit(limit).collect(java.util.stream.Collectors.toList());
        }

        log.info("从数据库获取热门商品");
        List<Product> hotProducts = productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                        .eq(Product::getSaleStatus, 1)
                        .orderByDesc(Product::getSalesCount)
                        .last("LIMIT 20")
        );

        // 写入本地缓存
        hotProductsCache.put("hot", hotProducts);
        log.info("热门商品已缓存到本地");

        return hotProducts.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 清除用户的推荐缓存
     */
    private void clearUserRecommendationCache(Long userId) {
        smartRecommendationCache.invalidate(userId);
        collocationCache.invalidate(userId);
        log.info("清除用户推荐缓存 - userId: {}", userId);
    }

    /**
     * 手动刷新热门商品缓存（可定时调用）
     */
    public void refreshHotProductsCache() {
        hotProductsCache.invalidate("hot");
        log.info("热门商品本地缓存已清除，下次访问将重新加载");
    }

    /**
     * 获取缓存统计信息（用于监控）
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("smartRecommendationCacheSize", smartRecommendationCache.estimatedSize());
        stats.put("collocationCacheSize", collocationCache.estimatedSize());
        stats.put("hotProductsCacheSize", hotProductsCache.estimatedSize());
        return stats;
    }

    private List<Product> getSimilarProducts(List<Long> viewedProductIds) {
        // 优化实现：基于多维度相似度推荐商品
        List<Product> similarProducts = new ArrayList<>();
        
        for (Long productId : viewedProductIds) {
            Product viewedProduct = productMapper.selectById(productId);
            if (viewedProduct == null || viewedProduct.getCategoryId() == null) {
                continue;
            }
            
            // 1. 同分类推荐（权重：高）
            List<Product> categoryProducts = productMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                            .eq(Product::getCategoryId, viewedProduct.getCategoryId())
                            .ne(Product::getId, productId)
                            .eq(Product::getSaleStatus, 1)
                            .orderByDesc(Product::getSalesCount)
                            .last("LIMIT 3")
            );
            
            // 2. 同农户推荐（权重：中）
            if (viewedProduct.getFarmerId() != null) {
                List<Product> farmerProducts = productMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                                .eq(Product::getFarmerId, viewedProduct.getFarmerId())
                                .ne(Product::getId, productId)
                                .eq(Product::getSaleStatus, 1)
                                .ne(Product::getCategoryId, viewedProduct.getCategoryId())  // 排除同分类已推荐的
                                .last("LIMIT 2")
                );
                similarProducts.addAll(farmerProducts);
            }
            
            // 3. 同价格区间推荐（权重：低）
            BigDecimal price = viewedProduct.getPrice();
            if (price != null) {
                BigDecimal minPrice = price.multiply(new java.math.BigDecimal("0.8"));
                BigDecimal maxPrice = price.multiply(new java.math.BigDecimal("1.2"));
                
                List<Product> priceRangeProducts = productMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                                .ge(Product::getPrice, minPrice)
                                .le(Product::getPrice, maxPrice)
                                .ne(Product::getId, productId)
                                .eq(Product::getSaleStatus, 1)
                                .ne(Product::getCategoryId, viewedProduct.getCategoryId())  // 排除同分类
                                .orderByDesc(Product::getSalesCount)
                                .last("LIMIT 2")
                );
                similarProducts.addAll(priceRangeProducts);
            }
            
            similarProducts.addAll(categoryProducts);
        }
        
        // 去重并限制数量，优先保留销量高的
        return similarProducts.stream()
                .distinct()
                .sorted((p1, p2) -> Integer.compare(
                        p2.getSalesCount() != null ? p2.getSalesCount() : 0,
                        p1.getSalesCount() != null ? p1.getSalesCount() : 0
                ))
                .limit(8)
                .collect(java.util.stream.Collectors.toList());
    }

    @SuppressWarnings("unused")
    private List<Product> getHotProducts(Integer limit) {
        return getHotProductsFromCache(limit);
    }

    private List<Map<String, Object>> convertToProductList(List<Product> products) {
        return products.stream()
                .map(this::convertToProductMap)
                .collect(java.util.stream.Collectors.toList());
    }

    private Map<String, Object> convertToProductMap(Product product) {
        Map<String, Object> map = new HashMap<>();
        
        // 基础信息
        map.put("id", product.getId());
        map.put("name", product.getProductName());
        map.put("price", product.getPrice());
        map.put("unit", product.getUnitName() != null ? product.getUnitName() : "斤");
        
        // 图片URL - 确保完整路径
        String coverImage = product.getCoverImage();
        if (coverImage != null && !coverImage.isEmpty()) {
            if (coverImage.startsWith("http")) {
                map.put("image", coverImage);
            } else {
                map.put("image", UrlUtils.toFullUrl(coverImage));
            }
        } else {
            map.put("image", UrlUtils.toFullUrl("/uploads/default-product.png"));
        }
        
        // 分类信息
        map.put("categoryId", product.getCategoryId());
        if (product.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
            map.put("categoryName", category != null ? category.getCategoryName() : "未分类");
        } else {
            map.put("categoryName", "未分类");
        }
        
        // 农户信息
        map.put("farmerId", product.getFarmerId());
        
        // 销售信息
        map.put("salesCount", product.getSalesCount() != null ? product.getSalesCount() : 0);
        map.put("stock", product.getStock() != null ? product.getStock() : 0);
        
        // 来源地
        map.put("originPlace", product.getOriginPlace());
        
        // 商品状态
        map.put("saleStatus", product.getSaleStatus());
        
        return map;
    }
    
    /**
     * 批量转换商品列表（优化：批量查询分类名称）
     */
    @SuppressWarnings("unused")
    private List<Map<String, Object>> convertToProductListWithBatchCategory(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量查询所有涉及的分类ID
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        
        // 批量获取分类信息
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<ProductCategory> categories = productMapper.selectBatchIds(categoryIds)
                    .stream()
                    .map(p -> productCategoryMapper.selectById(p.getCategoryId()))
                    .filter(c -> c != null)
                    .collect(java.util.stream.Collectors.toList());
            
            for (ProductCategory cat : categories) {
                categoryNameMap.put(cat.getId(), cat.getCategoryName());
            }
        }
        
        // 转换商品列表
        return products.stream().map(product -> {
            Map<String, Object> map = new HashMap<>();
            
            // 基础信息
            map.put("id", product.getId());
            map.put("name", product.getProductName());
            map.put("price", product.getPrice());
            map.put("unit", product.getUnitName() != null ? product.getUnitName() : "斤");
            
            // 图片URL - 确保完整路径
            String coverImage = product.getCoverImage();
            if (coverImage != null && !coverImage.isEmpty()) {
                if (coverImage.startsWith("http")) {
                    map.put("image", coverImage);
                } else {
                    map.put("image", UrlUtils.toFullUrl(coverImage));
                }
            } else {
                map.put("image", UrlUtils.toFullUrl("/uploads/default-product.png"));
            }
            
            // 分类信息（从缓存Map中获取）
            map.put("categoryId", product.getCategoryId());
            if (product.getCategoryId() != null) {
                map.put("categoryName", categoryNameMap.getOrDefault(product.getCategoryId(), "未分类"));
            } else {
                map.put("categoryName", "未分类");
            }
            
            // 其他信息
            map.put("farmerId", product.getFarmerId());
            map.put("salesCount", product.getSalesCount() != null ? product.getSalesCount() : 0);
            map.put("stock", product.getStock() != null ? product.getStock() : 0);
            map.put("originPlace", product.getOriginPlace());
            map.put("saleStatus", product.getSaleStatus());
            
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }
}
