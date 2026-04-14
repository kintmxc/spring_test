package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recommendation_log")
public class RecommendationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer recommendationType;
    private String recommendationData;
    private LocalDateTime recommendationTime;
    private Integer clickCount;
    private Integer purchaseCount;
}
