package com.example.spring_test.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardOverviewVO {
    private long todayOrderCount;
    private long pendingOrderCount;
    private long onSaleProductCount;
    private BigDecimal monthSalesAmount;
}