package com.example.spring_test.service;

import com.example.spring_test.vo.DashboardOverviewVO;
import com.example.spring_test.vo.LatestOrderVO;
import java.util.List;

public interface DashboardService {
    DashboardOverviewVO overview();

    List<LatestOrderVO> latestOrders(int limit);
}