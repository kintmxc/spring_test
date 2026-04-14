package com.example.spring_test.service;

import java.util.Map;

public interface AiSalesAnalysisService {
    Map<String, Object> analyze(Long farmerId, String dateType);
}
