package com.example.spring_test.security;

public final class SessionUserHolder {
    private static final ThreadLocal<SessionUser> HOLDER = new ThreadLocal<>();

    private SessionUserHolder() {
    }

    public static void set(SessionUser user) {
        HOLDER.set(user);
    }

    public static SessionUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}