package com.example.spring_test.enums;

public enum FarmerAuthStatusEnum {
    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    private final int code;

    FarmerAuthStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}