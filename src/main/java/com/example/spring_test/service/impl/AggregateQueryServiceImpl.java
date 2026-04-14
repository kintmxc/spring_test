package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.OrderLogistics;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductTrace;
import com.example.spring_test.entity.UserAddress;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrderLogisticsMapper;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.ProductTraceMapper;
import com.example.spring_test.mapper.UserAddressMapper;
import com.example.spring_test.service.AggregateQueryService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AggregateQueryServiceImpl implements AggregateQueryService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final FarmerMapper farmerMapper;
    private final UserAddressMapper addressMapper;
    private final ProductTraceMapper traceMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogisticsMapper logisticsMapper;

    public AggregateQueryServiceImpl(ProductMapper productMapper,
                                      ProductCategoryMapper categoryMapper,
                                      FarmerMapper farmerMapper,
                                      UserAddressMapper addressMapper,
                                      ProductTraceMapper traceMapper,
                                      OrderItemMapper orderItemMapper,
                                      OrderLogisticsMapper logisticsMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.farmerMapper = farmerMapper;
        this.addressMapper = addressMapper;
        this.traceMapper = traceMapper;
        this.orderItemMapper = orderItemMapper;
        this.logisticsMapper = logisticsMapper;
    }

    @Override
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public ProductCategory getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public Farmer getFarmerById(Long id) {
        return farmerMapper.selectById(id);
    }

    @Override
    public UserAddress getAddressById(Long id) {
        return addressMapper.selectById(id);
    }

    @Override
    public ProductTrace getTraceByProductId(Long productId) {
        return traceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, productId)
                .last("limit 1"));
    }

    @Override
    public OrderLogistics getLogisticsByOrderId(Long orderId) {
        return logisticsMapper.selectOne(new LambdaQueryWrapper<OrderLogistics>()
                .eq(OrderLogistics::getOrderId, orderId)
                .last("limit 1"));
    }

    @Override
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    @Override
    public Map<Long, String> getCategoryNameMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName));
    }

    @Override
    public Map<Long, String> getFarmerNameMap(Set<Long> farmerIds) {
        if (farmerIds == null || farmerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return farmerMapper.selectBatchIds(farmerIds).stream()
                .collect(Collectors.toMap(Farmer::getId, Farmer::getFarmerName));
    }

    @Override
    public Map<Long, Farmer> getFarmerMap(Set<Long> farmerIds) {
        if (farmerIds == null || farmerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return farmerMapper.selectBatchIds(farmerIds).stream()
                .collect(Collectors.toMap(Farmer::getId, Function.identity()));
    }

    @Override
    public Map<Long, ProductCategory> getCategoryMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, Function.identity()));
    }

    @Override
    @Transactional
    public void updateProductStock(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setStock(product.getStock() + quantity);
            productMapper.updateById(product);
        }
    }

    @Override
    @Transactional
    public void updateProductSales(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            int currentSales = product.getSalesCount() == null ? 0 : product.getSalesCount();
            product.setSalesCount(currentSales + quantity);
            productMapper.updateById(product);
        }
    }

    @Override
    @Transactional
    public boolean decreaseProductStockAndIncreaseSales(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return false;
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("stock = stock - " + quantity)
                .setSql("sales_count = sales_count + " + quantity)
                .eq(Product::getId, productId)
                .ge(Product::getStock, quantity);
        return productMapper.update(null, updateWrapper) > 0;
    }

    @Override
    @Transactional
    public boolean increaseProductStockAndDecreaseSales(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return false;
        }
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("stock = IFNULL(stock, 0) + " + quantity)
                .setSql("sales_count = GREATEST(IFNULL(sales_count, 0) - " + quantity + ", 0)")
                .eq(Product::getId, productId);
        return productMapper.update(null, updateWrapper) > 0;
    }
}
