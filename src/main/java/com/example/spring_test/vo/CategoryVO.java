package com.example.spring_test.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CategoryVO {
    private Long id;
    private String categoryName;
    private Integer sortNo;
    private Integer status;
    private String statusText;
    private Integer productCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}