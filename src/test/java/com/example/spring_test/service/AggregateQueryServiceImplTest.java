package com.example.spring_test.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrderLogisticsMapper;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.ProductTraceMapper;
import com.example.spring_test.mapper.UserAddressMapper;
import com.example.spring_test.service.impl.AggregateQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateQueryServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductCategoryMapper categoryMapper;

    @Mock
    private FarmerMapper farmerMapper;

    @Mock
    private UserAddressMapper addressMapper;

    @Mock
    private ProductTraceMapper traceMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OrderLogisticsMapper logisticsMapper;

    @InjectMocks
    private AggregateQueryServiceImpl aggregateQueryService;

    @BeforeEach
    void setUp() {
        aggregateQueryService = new AggregateQueryServiceImpl(
                productMapper,
                categoryMapper,
                farmerMapper,
                addressMapper,
                traceMapper,
                orderItemMapper,
                logisticsMapper
        );
    }

    @Test
    void decreaseProductStockAndIncreaseSales_shouldReturnTrue_whenUpdateSuccess() {
        when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean success = aggregateQueryService.decreaseProductStockAndIncreaseSales(1L, 2);

        assertTrue(success);
    }

    @Test
    void decreaseProductStockAndIncreaseSales_shouldReturnFalse_whenInvalidArgs() {
        assertFalse(aggregateQueryService.decreaseProductStockAndIncreaseSales(null, 2));
        assertFalse(aggregateQueryService.decreaseProductStockAndIncreaseSales(1L, null));
        assertFalse(aggregateQueryService.decreaseProductStockAndIncreaseSales(1L, 0));
    }

    @Test
    void increaseProductStockAndDecreaseSales_shouldReturnFalse_whenProductNotFound() {
        when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        boolean success = aggregateQueryService.increaseProductStockAndDecreaseSales(1L, 3);

        assertFalse(success);
    }

    @Test
    void increaseProductStockAndDecreaseSales_shouldReturnTrue_whenUpdateSuccess() {
        when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean success = aggregateQueryService.increaseProductStockAndDecreaseSales(1L, 3);

        assertTrue(success);
        verify(productMapper).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void increaseProductStockAndDecreaseSales_shouldReturnFalse_whenInvalidArgs() {
        assertFalse(aggregateQueryService.increaseProductStockAndDecreaseSales(null, 2));
        assertFalse(aggregateQueryService.increaseProductStockAndDecreaseSales(1L, null));
        assertFalse(aggregateQueryService.increaseProductStockAndDecreaseSales(1L, 0));
    }
}
