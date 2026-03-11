package com.example.spring_test.enums;

public enum OrderStatusEnum {
    CREATED(0),
    PAID(1),
    SHIPPED(2),
    COMPLETED(3),
    CANCELED(4);

    private final int code;

    OrderStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}