package com.example.spring_test.vo;

import com.example.spring_test.entity.OrderItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long farmerId;
    private String farmerName;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer orderStatus;
    private String orderStatusText;
    private Integer payStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String logisticsCompany;
    private String trackingNo;
    private Integer logisticsStatus;
    private String shipRemark;
    private List<OrderItem> items;
}