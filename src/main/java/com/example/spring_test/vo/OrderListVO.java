package com.example.spring_test.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderListVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long farmerId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer orderStatus;
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
    private String farmerName;
    private Integer itemCount;
    private String itemSummary;
    private String logisticsCompany;
    private String trackingNo;
}