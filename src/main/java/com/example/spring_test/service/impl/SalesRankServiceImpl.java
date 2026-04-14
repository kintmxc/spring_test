package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.service.AggregateQueryService;
import com.example.spring_test.service.SalesRankService;
import com.example.spring_test.util.UrlUtils;
import com.example.spring_test.vo.SalesRankVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesRankServiceImpl implements SalesRankService {
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrdersMapper ordersMapper;
    private final AggregateQueryService aggregateQueryService;

    public SalesRankServiceImpl(ProductMapper productMapper, OrderItemMapper orderItemMapper,
                                OrdersMapper ordersMapper, AggregateQueryService aggregateQueryService) {
        this.productMapper = productMapper;
        this.orderItemMapper = orderItemMapper;
        this.ordersMapper = ordersMapper;
        this.aggregateQueryService = aggregateQueryService;
    }

    @Override
    public List<SalesRankVO> getSalesRank(String type, Integer limit) {
        int limitValue = limit != null && limit > 0 ? limit : 10;
        LocalDateTime startTime = calculateStartTime(type);
        
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .ge(startTime != null, Orders::getCreateTime, startTime)
                .in(Orders::getOrderStatus, 1, 2, 3));
        
        if (orders.isEmpty()) {
            return getTopProductsBySalesCount(limitValue);
        }
        
        Set<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toSet());
        
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));
        
        Map<Long, Integer> productSales = new HashMap<>();
        Map<Long, BigDecimal> productRevenue = new HashMap<>();
        for (OrderItem item : orderItems) {
            productSales.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            BigDecimal revenue = item.getSubtotalAmount();
            productRevenue.merge(item.getProductId(), revenue, BigDecimal::add);
        }
        
        if (productSales.isEmpty()) {
            return getTopProductsBySalesCount(limitValue);
        }
        
        List<Long> topProductIds = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limitValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
            .in(Product::getId, topProductIds)
            .eq(Product::getSaleStatus, 1));
        
        Set<Long> farmerIds = products.stream()
            .map(Product::getFarmerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Farmer> farmerMap = aggregateQueryService.getFarmerMap(farmerIds);
        
        return products.stream()
                .map(product -> {
                    SalesRankVO vo = new SalesRankVO();
                    vo.setId(product.getId());
                    vo.setName(product.getProductName());
                    vo.setPrice(product.getPrice());
                    vo.setUnit(product.getUnitName());
                    vo.setImage(UrlUtils.toFullUrl(product.getCoverImage()));
                    Farmer farmer = farmerMap.get(product.getFarmerId());
                    vo.setFarmerName(farmer != null ? farmer.getFarmerName() : "未知农户");
                    vo.setSalesVolume(productSales.getOrDefault(product.getId(), product.getSalesCount()));
                    vo.setSalesAmount(productRevenue.getOrDefault(product.getId(), BigDecimal.ZERO));
                    return vo;
                })
                .sorted(Comparator.comparing(SalesRankVO::getSalesVolume).reversed())
                .collect(Collectors.toList());
    }
    
    private List<SalesRankVO> getTopProductsBySalesCount(int limit) {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSaleStatus, 1)
                .orderByDesc(Product::getSalesCount)
                .last("limit " + limit));

        Set<Long> farmerIds = products.stream()
                .map(Product::getFarmerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Farmer> farmerMap = aggregateQueryService.getFarmerMap(farmerIds);
        
        return products.stream()
                .map(product -> {
                    SalesRankVO vo = new SalesRankVO();
                    vo.setId(product.getId());
                    vo.setName(product.getProductName());
                    vo.setPrice(product.getPrice());
                    vo.setUnit(product.getUnitName());
                    vo.setImage(UrlUtils.toFullUrl(product.getCoverImage()));
                    Farmer farmer = farmerMap.get(product.getFarmerId());
                    vo.setFarmerName(farmer != null ? farmer.getFarmerName() : "未知农户");
                    int salesCount = product.getSalesCount() != null ? product.getSalesCount() : 0;
                    vo.setSalesVolume(salesCount);
                    // 计算销售额（销量 * 价格）
                    BigDecimal salesAmount = product.getPrice().multiply(new BigDecimal(salesCount));
                    vo.setSalesAmount(salesAmount);
                    return vo;
                })
                .collect(Collectors.toList());
    }
    
    private LocalDateTime calculateStartTime(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return switch (type.toLowerCase()) {
            case "day" -> now.minusDays(1);
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            default -> null;
        };
    }
}
