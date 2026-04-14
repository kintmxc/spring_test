package com.example.spring_test.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class FarmerSalesRankVO {
    private BigDecimal totalSales;
    private Integer totalOrders;
    private Integer totalQuantity;
    private List<SalesTrendVO> salesTrend;
    private List<FarmerProductRankVO> productRank;
    
    @Data
    public static class SalesTrendVO {
        private String day;
        private BigDecimal value;
    }
    
    @Data
    public static class FarmerProductRankVO {
        private Long id;
        private String name;
        private String image;
        private Integer salesCount;
        private BigDecimal revenue;
    }
}