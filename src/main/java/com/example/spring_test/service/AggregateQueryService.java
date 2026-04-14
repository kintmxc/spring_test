package com.example.spring_test.service;

import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.OrderLogistics;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductTrace;
import com.example.spring_test.entity.UserAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AggregateQueryService {
    
    Product getProductById(Long id);
    
    ProductCategory getCategoryById(Long id);
    
    Farmer getFarmerById(Long id);
    
    UserAddress getAddressById(Long id);
    
    ProductTrace getTraceByProductId(Long productId);
    
    OrderLogistics getLogisticsByOrderId(Long orderId);
    
    List<OrderItem> getOrderItemsByOrderId(Long orderId);
    
    Map<Long, String> getCategoryNameMap(Set<Long> categoryIds);
    
    Map<Long, String> getFarmerNameMap(Set<Long> farmerIds);
    
    Map<Long, Farmer> getFarmerMap(Set<Long> farmerIds);
    
    Map<Long, ProductCategory> getCategoryMap(Set<Long> categoryIds);
    
    void updateProductStock(Long productId, Integer quantity);
    
    void updateProductSales(Long productId, Integer quantity);

    boolean decreaseProductStockAndIncreaseSales(Long productId, Integer quantity);

    boolean increaseProductStockAndDecreaseSales(Long productId, Integer quantity);
}
