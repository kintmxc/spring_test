package com.example.spring_test.service;

import com.example.spring_test.dto.EvaluationSaveDTO;
import java.util.Map;

public interface EvaluationService {
    Map<String, Object> create(EvaluationSaveDTO evaluationSaveDTO);

    Map<String, Object> list(Long productId);
    
    Map<String, Object> list(Long productId, Integer page, Integer pageSize);
}
