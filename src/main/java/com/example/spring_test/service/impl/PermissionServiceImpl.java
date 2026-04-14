package com.example.spring_test.service.impl;

import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.exception.ForbiddenException;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Override
    public void requireAdmin(String message) {
        CurrentUserUtil.requireAdmin(message);
    }

    @Override
    public void requireAdminOrOwner(Long ownerId, String message) {
        CurrentUserUtil.requireAdminOrOwner(ownerId, message);
    }

    @Override
    public boolean isAdmin() {
        return CurrentUserUtil.isAdmin();
    }

    @Override
    public boolean isFarmer() {
        return CurrentUserUtil.isFarmer();
    }

    @Override
    public boolean isConsumer() {
        return CurrentUserUtil.isConsumer();
    }

    @Override
    public Long getCurrentUserId() {
        return CurrentUserUtil.currentUserId();
    }

    @Override
    public boolean canManageProduct(Product product) {
        if (isAdmin()) {
            return true;
        }
        if (isFarmer()) {
            return getCurrentUserId().equals(product.getFarmerId());
        }
        return false;
    }

    @Override
    public boolean canManageOrder(Orders order) {
        if (isAdmin()) {
            return true;
        }
        if (isFarmer()) {
            return getCurrentUserId().equals(order.getFarmerId());
        }
        return false;
    }

    @Override
    public boolean canViewOrder(Orders order) {
        if (isAdmin()) {
            return true;
        }
        Long currentUserId = getCurrentUserId();
        if (isFarmer() && currentUserId.equals(order.getFarmerId())) {
            return true;
        }
        if (isConsumer() && currentUserId.equals(order.getUserId())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canManageFarmer(Farmer farmer) {
        return isAdmin();
    }
    
    public void checkProductAccess(Product product, String message) {
        if (!canManageProduct(product)) {
            throw new ForbiddenException(message);
        }
    }
    
    public void checkOrderAccess(Orders order, String message) {
        if (!canViewOrder(order)) {
            throw new ForbiddenException(message);
        }
    }
    
    public void checkOrderManageAccess(Orders order, String message) {
        if (!canManageOrder(order)) {
            throw new ForbiddenException(message);
        }
    }
}
