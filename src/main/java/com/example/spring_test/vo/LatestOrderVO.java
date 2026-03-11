package com.example.spring_test.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LatestOrderVO {
    private Long id;
    private String orderNo;
    private Long farmerId;
    private String receiverName;
    private BigDecimal payAmount;
    private Integer orderStatus;
    private LocalDateTime createTime;
}