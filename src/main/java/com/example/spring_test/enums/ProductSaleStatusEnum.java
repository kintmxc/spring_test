package com.example.spring_test.enums;

public enum ProductSaleStatusEnum {
    OFF_SHELF(0),
    ON_SHELF(1);

    private final int code;

    ProductSaleStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}