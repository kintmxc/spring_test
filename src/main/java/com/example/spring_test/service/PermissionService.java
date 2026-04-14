package com.example.spring_test.service;

import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;

public interface PermissionService {
    
    void requireAdmin(String message);
    
    void requireAdminOrOwner(Long ownerId, String message);
    
    boolean isAdmin();
    
    boolean isFarmer();
    
    boolean isConsumer();
    
    Long getCurrentUserId();
    
    boolean canManageProduct(Product product);
    
    boolean canManageOrder(Orders order);
    
    boolean canViewOrder(Orders order);
    
    boolean canManageFarmer(Farmer farmer);
}
