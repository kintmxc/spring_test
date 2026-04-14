package com.example.spring_test.security;

import com.example.spring_test.enums.RoleEnum;
import com.example.spring_test.exception.ForbiddenException;
import com.example.spring_test.exception.UnauthorizedException;

public final class CurrentUserUtil {
    private CurrentUserUtil() {
    }

    public static SessionUser getRequiredUser() {
        SessionUser user = SessionUserHolder.get();
        if (user == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return user;
    }

    public static boolean isAdmin() {
        return RoleEnum.ADMIN.name().equalsIgnoreCase(getRequiredUser().getRoleCode());
    }

    public static boolean isFarmer() {
        return RoleEnum.FARMER.name().equalsIgnoreCase(getRequiredUser().getRoleCode());
    }

    public static boolean isConsumer() {
        return RoleEnum.CONSUMER.name().equalsIgnoreCase(getRequiredUser().getRoleCode());
    }

    public static void requireAdmin(String message) {
        if (!isAdmin()) {
            throw new ForbiddenException(message);
        }
    }

    public static Long currentUserId() {
        return getRequiredUser().getUserId();
    }

    public static String getCurrentUserType() {
        String roleCode = getRequiredUser().getRoleCode();
        if (RoleEnum.ADMIN.name().equalsIgnoreCase(roleCode)) {
            return "管理员";
        }
        if (RoleEnum.FARMER.name().equalsIgnoreCase(roleCode)) {
            return "农户";
        }
        if (RoleEnum.CONSUMER.name().equalsIgnoreCase(roleCode)) {
            return "消费者";
        }
        return "未知";
    }

    public static void requireAdminOrOwner(Long ownerId, String message) {
        if (!isAdmin() && !currentUserId().equals(ownerId)) {
            throw new ForbiddenException(message);
        }
    }
}