package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.DashboardService;
import com.example.spring_test.vo.DashboardOverviewVO;
import com.example.spring_test.vo.LatestOrderVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final OrdersMapper ordersMapper;
    private final ProductMapper productMapper;

    public DashboardServiceImpl(OrdersMapper ordersMapper, ProductMapper productMapper) {
        this.ordersMapper = ordersMapper;
        this.productMapper = productMapper;
    }

    @Override
    public DashboardOverviewVO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        Long farmerId = CurrentUserUtil.isFarmer() ? CurrentUserUtil.currentUserId() : null;
        long todayOrderCount = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(farmerId != null, Orders::getFarmerId, farmerId)
                .between(Orders::getCreateTime, todayStart, todayEnd));

        long pendingOrderCount = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(farmerId != null, Orders::getFarmerId, farmerId)
                .in(Orders::getOrderStatus, 0, 1));

        long onSaleProductCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(farmerId != null, Product::getFarmerId, farmerId)
                .eq(Product::getSaleStatus, 1));

        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);
        List<Orders> monthOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(farmerId != null, Orders::getFarmerId, farmerId)
                .between(Orders::getPayTime, monthStart, monthEnd)
                .in(Orders::getOrderStatus, 1, 2, 3));
        BigDecimal monthSalesAmount = monthOrders.stream()
                .map(Orders::getPayAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardOverviewVO(todayOrderCount, pendingOrderCount, onSaleProductCount, monthSalesAmount);
    }

        @Override
        public Map<String, Object> salesStats(String period) {
                Long farmerId = CurrentUserUtil.isFarmer() ? CurrentUserUtil.currentUserId() : null;
                List<Orders> paidOrders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                                .eq(farmerId != null, Orders::getFarmerId, farmerId)
                                .in(Orders::getOrderStatus, 1, 2, 3));
                BigDecimal totalSales = paidOrders.stream()
                                .map(Orders::getPayAmount)
                                .filter(amount -> amount != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                int totalOrders = paidOrders.size();
                int totalQuantity = totalOrders;

                Map<String, Object> result = new HashMap<>();
                result.put("period", period == null ? "month" : period);
                result.put("totalSales", totalSales);
                result.put("totalOrders", totalOrders);
                result.put("totalQuantity", totalQuantity);
                result.put("salesTrend", 0);
                result.put("orderTrend", 0);
                result.put("dailySales", new ArrayList<>());
                result.put("productRank", new ArrayList<>());
                return result;
        }

    @Override
    public List<LatestOrderVO> latestOrders(int limit) {
        int size = limit <= 0 ? 5 : Math.min(limit, 20);
        Long farmerId = CurrentUserUtil.isFarmer() ? CurrentUserUtil.currentUserId() : null;
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(farmerId != null, Orders::getFarmerId, farmerId)
                .orderByDesc(Orders::getCreateTime)
                .last("limit " + size));
        return orders.stream()
                .map(order -> new LatestOrderVO(
                        order.getId(),
                        order.getOrderNo(),
                        order.getFarmerId(),
                        order.getReceiverName(),
                        order.getPayAmount(),
                        order.getOrderStatus(),
                        order.getCreateTime()))
                .toList();
    }
}