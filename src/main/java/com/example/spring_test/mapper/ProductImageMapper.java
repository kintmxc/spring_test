package com.example.spring_test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spring_test.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
}
