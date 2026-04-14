package com.example.spring_test.service;

import com.example.spring_test.vo.FarmerSalesRankVO;

public interface FarmerSalesRankService {
    FarmerSalesRankVO getFarmerSalesRank(String type, Integer limit);
}