package com.example.spring_test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spring_test.entity.ProductCollocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductCollocationMapper extends BaseMapper<ProductCollocation> {
    
    /**
     * 获取商品的搭配推荐
     */
    List<ProductCollocation> getCollocationsByProductId(@Param("productId") Long productId, @Param("limit") Integer limit);
    
    /**
     * 获取高分搭配推荐
     */
    List<ProductCollocation> getTopCollocations(@Param("limit") Integer limit);
}
