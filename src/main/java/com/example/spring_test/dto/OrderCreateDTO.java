package com.example.spring_test.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class OrderCreateDTO {
    private Long productId;
    
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;
    
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
    
    private String remark;
    
    @Valid
    private List<OrderItemDTO> items;
    
    @Data
    public static class OrderItemDTO {
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        
        @Min(value = 1, message = "购买数量必须大于0")
        private Integer quantity;
    }
}
