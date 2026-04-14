package com.example.spring_test.dto;

import java.util.List;
import lombok.Data;

@Data
public class EvaluationSaveDTO {
    private Long orderId;
    private Long productId;
    private Integer score;
    private String content;
    private List<String> images;
    private List<String> tags;
}
