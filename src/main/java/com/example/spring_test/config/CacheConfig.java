package com.example.spring_test.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 推荐缓存配置 - 5分钟过期
     */
    @Bean
    public Caffeine<Object, Object> recommendationCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .initialCapacity(50)
                .maximumSize(200)
                .recordStats();
    }

    /**
     * 搭配缓存配置 - 10分钟过期
     */
    @Bean
    public Caffeine<Object, Object> collocationCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .initialCapacity(50)
                .maximumSize(200)
                .recordStats();
    }

    /**
     * 热门商品缓存配置 - 30分钟过期
     */
    @Bean
    public Caffeine<Object, Object> hotProductsCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .initialCapacity(10)
                .maximumSize(50)
                .recordStats();
    }

    /**
     * 主缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .initialCapacity(100)
                .maximumSize(1000));
        return cacheManager;
    }

    /**
     * 推荐专用缓存管理器
     */
    @Bean
    public CacheManager recommendationCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("recommendation", "collocation", "hotProducts");
        cacheManager.setCaffeine(recommendationCaffeine());
        return cacheManager;
    }
}
