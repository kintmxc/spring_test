package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.service.DashboardService;
import com.example.spring_test.vo.DashboardOverviewVO;
import com.example.spring_test.vo.LatestOrderVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @GetMapping("/latest-orders")
    public Result<List<LatestOrderVO>> latestOrders(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(dashboardService.latestOrders(limit));
    }
}