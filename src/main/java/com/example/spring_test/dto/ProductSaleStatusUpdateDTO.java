package com.example.spring_test.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class ProductSaleStatusUpdateDTO {
    @JsonAlias({"saleStatus", "sale_status"})
    private Integer status;
}
