package com.example.spring_test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spring_test.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
    
    /**
     * 获取用户的最新行为记录
     */
    List<UserBehavior> getRecentBehaviorsByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 获取用户浏览过的商品ID列表
     */
    List<Long> getViewedProductIdsByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
}
