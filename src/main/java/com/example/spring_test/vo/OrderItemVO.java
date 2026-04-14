package com.example.spring_test.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private String productImage;
    private String productUnit;
    private String productOrigin;
}
