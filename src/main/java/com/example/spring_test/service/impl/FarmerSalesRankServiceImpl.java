package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.FarmerSalesRankService;
import com.example.spring_test.util.UrlUtils;
import com.example.spring_test.vo.FarmerSalesRankVO;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FarmerSalesRankServiceImpl implements FarmerSalesRankService {
    
    @Resource
    private OrdersMapper ordersMapper;
    
    @Resource
    private OrderItemMapper orderItemMapper;
    
    @Resource
    private ProductMapper productMapper;
    
    @Resource
    private FarmerMapper farmerMapper;
    
    @Override
    public FarmerSalesRankVO getFarmerSalesRank(String type, Integer limit) {
        Long farmerId = CurrentUserUtil.currentUserId();
        int limitValue = limit != null && limit > 0 ? limit : 5;
        
        LocalDateTime startTime = calculateStartTime(type);
        
        // 统计当前农户的订单
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getFarmerId, farmerId)
                .ge(startTime != null, Orders::getCreateTime, startTime)
                .in(Orders::getOrderStatus, 1, 2, 3));
        
        FarmerSalesRankVO result = new FarmerSalesRankVO();
        
        if (orders.isEmpty()) {
            result.setTotalSales(BigDecimal.ZERO);
            result.setTotalOrders(0);
            result.setTotalQuantity(0);
            result.setSalesTrend(Collections.emptyList());
            result.setProductRank(Collections.emptyList());
            return result;
        }
        
        // 计算总销售额和总订单数
        BigDecimal totalSales = orders.stream()
                .map(Orders::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalOrders = orders.size();
        
        // 计算总销量和商品销量
        Set<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toSet());
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));
        
        int totalQuantity = orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        
        // 计算商品销量和销售额
        Map<Long, Integer> productSales = new HashMap<>();
        Map<Long, BigDecimal> productRevenue = new HashMap<>();
        for (OrderItem item : orderItems) {
            productSales.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            productRevenue.merge(item.getProductId(), item.getSubtotalAmount(), BigDecimal::add);
        }
        
        // 生成销售趋势
        List<FarmerSalesRankVO.SalesTrendVO> salesTrend = generateSalesTrend(orders, type);
        
        // 生成商品排行
        List<FarmerSalesRankVO.FarmerProductRankVO> productRank = generateProductRank(
                productSales, productRevenue, limitValue);
        
        result.setTotalSales(totalSales);
        result.setTotalOrders(totalOrders);
        result.setTotalQuantity(totalQuantity);
        result.setSalesTrend(salesTrend);
        result.setProductRank(productRank);
        
        return result;
    }
    
    private LocalDateTime calculateStartTime(String type) {
        LocalDateTime now = LocalDateTime.now();
        if ("week".equals(type)) {
            return now.minusDays(7);
        } else if ("month".equals(type)) {
            return now.minusMonths(1);
        } else if ("year".equals(type)) {
            return now.minusYears(1);
        }
        return now.minusMonths(1); // 默认一个月
    }
    
    private List<FarmerSalesRankVO.SalesTrendVO> generateSalesTrend(List<Orders> orders, String type) {
        List<FarmerSalesRankVO.SalesTrendVO> trend = new ArrayList<>();
        
        if (orders.isEmpty()) {
            return trend;
        }
        
        // 按日期分组
        Map<String, BigDecimal> dailySales = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (Orders order : orders) {
            String dateStr = order.getCreateTime().format(formatter);
            BigDecimal amount = order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO;
            dailySales.merge(dateStr, amount, BigDecimal::add);
        }
        
        // 生成最近7天的趋势
        LocalDateTime now = LocalDateTime.now();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            String dateStr = date.format(formatter);
            String dayName = getDayName(i);
            
            FarmerSalesRankVO.SalesTrendVO vo = new FarmerSalesRankVO.SalesTrendVO();
            vo.setDay(dayName);
            vo.setValue(dailySales.getOrDefault(dateStr, BigDecimal.ZERO));
            trend.add(vo);
        }
        
        return trend;
    }
    
    private String getDayName(int daysAgo) {
        String[] dayNames = {"今天", "昨天", "前天", "3天前", "4天前", "5天前", "6天前"};
        return dayNames[Math.min(daysAgo, dayNames.length - 1)];
    }
    
    private List<FarmerSalesRankVO.FarmerProductRankVO> generateProductRank(
            Map<Long, Integer> productSales, Map<Long, BigDecimal> productRevenue, int limit) {
        if (productSales.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> topProductIds = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .in(Product::getId, topProductIds));
        
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        
        return topProductIds.stream()
                .map(productId -> {
                    Product product = productMap.get(productId);
                    if (product == null) {
                        return null;
                    }
                    
                    FarmerSalesRankVO.FarmerProductRankVO vo = new FarmerSalesRankVO.FarmerProductRankVO();
                    vo.setId(product.getId());
                    vo.setName(product.getProductName());
                    vo.setImage(UrlUtils.toFullUrl(product.getCoverImage()));
                    vo.setSalesCount(productSales.getOrDefault(productId, 0));
                    vo.setRevenue(productRevenue.getOrDefault(productId, BigDecimal.ZERO));
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}