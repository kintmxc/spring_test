package com.example.spring_test.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OptionVO {
    private Long value;
    private String label;
}