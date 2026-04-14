package com.example.spring_test.service;

import com.example.spring_test.vo.DashboardOverviewVO;
import com.example.spring_test.vo.LatestOrderVO;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    DashboardOverviewVO overview();

    Map<String, Object> salesStats(String period);

    List<LatestOrderVO> latestOrders(int limit);
}