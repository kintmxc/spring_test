package com.example.spring_test.dto;

import lombok.Data;

@Data
public class CategorySaveDTO {
    private String categoryName;
    private Integer sortNo;
    private Integer status;
}