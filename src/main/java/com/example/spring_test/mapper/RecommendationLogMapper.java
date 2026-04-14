package com.example.spring_test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spring_test.entity.RecommendationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationLogMapper extends BaseMapper<RecommendationLog> {
    
    /**
     * 记录推荐日志
     */
    int insertLog(@Param("userId") Long userId, @Param("recommendationType") Integer recommendationType, @Param("recommendationData") String recommendationData);
}
