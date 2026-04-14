package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.service.FarmerSalesRankService;
import com.example.spring_test.vo.FarmerSalesRankVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farmer")
public class FarmerSalesRankController {
    private final FarmerSalesRankService farmerSalesRankService;

    public FarmerSalesRankController(FarmerSalesRankService farmerSalesRankService) {
        this.farmerSalesRankService = farmerSalesRankService;
    }

    @GetMapping("/sales-rank")
    public Result<FarmerSalesRankVO> getFarmerSalesRank(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit) {
        FarmerSalesRankVO rank = farmerSalesRankService.getFarmerSalesRank(type, limit);
        return Result.success(rank);
    }
}