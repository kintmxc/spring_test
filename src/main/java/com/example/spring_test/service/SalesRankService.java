package com.example.spring_test.service;

import com.example.spring_test.vo.SalesRankVO;
import java.util.List;

public interface SalesRankService {
    List<SalesRankVO> getSalesRank(String type, Integer limit);
}

