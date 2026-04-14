package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("order_logistics")
public class OrderLogistics {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String companyName;
    private String trackingNo;
    private Integer logisticsStatus;
    private String shipRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}