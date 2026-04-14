package com.example.spring_test.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.service.impl.AiSalesAnalysisServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiSalesAnalysisServiceImplTest {

    @Mock
    private OrdersMapper ordersMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private AiSalesAnalysisServiceImpl aiSalesAnalysisService;

    @Test
    void analyze_shouldReturnThreeSections_withStructuredContent() {
        Orders order = new Orders();
        order.setId(101L);
        order.setCreateTime(LocalDateTime.now().minusDays(1));

        Product fish = new Product();
        fish.setId(1L);
        fish.setFarmerId(9L);
        fish.setProductName("进口新鲜海鱼");
        fish.setPrice(new BigDecimal("13.5"));

        Product chicken = new Product();
        chicken.setId(2L);
        chicken.setFarmerId(9L);
        chicken.setProductName("散养土山鸡");
        chicken.setPrice(new BigDecimal("39.9"));

        OrderItem fishItem = new OrderItem();
        fishItem.setOrderId(101L);
        fishItem.setProductId(1L);
        fishItem.setQuantity(1);

        OrderItem chickenItem = new OrderItem();
        chickenItem.setOrderId(101L);
        chickenItem.setProductId(2L);
        chickenItem.setQuantity(6);

        when(ordersMapper.selectList(any())).thenReturn(List.of(order));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(fishItem, chickenItem));
        when(productMapper.selectList(any())).thenReturn(List.of(fish, chicken));

        Map<String, Object> result = aiSalesAnalysisService.analyze(9L, "week");

        assertNotNull(result.get("stagnantProduct"));
        assertNotNull(result.get("bestTime"));
        assertNotNull(result.get("seasonalProduct"));

        @SuppressWarnings("unchecked")
        Map<String, Object> stagnant = (Map<String, Object>) result.get("stagnantProduct");
        assertTrue(String.valueOf(stagnant.get("analysis")).contains("进口新鲜海鱼"));
        assertTrue(String.valueOf(stagnant.get("suggestions")).contains("销量较低"));

        @SuppressWarnings("unchecked")
        Map<String, Object> bestTime = (Map<String, Object>) result.get("bestTime");
        assertTrue(String.valueOf(bestTime.get("analysis")).contains("销售趋势"));

        @SuppressWarnings("unchecked")
        Map<String, Object> seasonal = (Map<String, Object>) result.get("seasonalProduct");
        assertTrue(String.valueOf(seasonal.get("analysis")).contains("当前月份"));
    }

    @Test
    void analyze_shouldReturnNoOrderGuidance_whenNoOrders() {
        Product product = new Product();
        product.setId(88L);
        product.setFarmerId(7L);
        product.setProductName("土鸡蛋");
        product.setPrice(new BigDecimal("9.9"));

        when(ordersMapper.selectList(any())).thenReturn(List.of());
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        Map<String, Object> result = aiSalesAnalysisService.analyze(7L, "month");

        @SuppressWarnings("unchecked")
        Map<String, Object> bestTime = (Map<String, Object>) result.get("bestTime");
        assertTrue(String.valueOf(bestTime.get("analysis")).contains("暂无成交订单"));
    }
}
