package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("product_trace")
public class ProductTrace {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private LocalDate productionDate;
    private String originDesc;
    private String inspectDesc;
    private Integer traceStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}