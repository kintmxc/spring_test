package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.service.SalesRankService;
import com.example.spring_test.vo.SalesRankVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SalesRankController {
    private final SalesRankService salesRankService;

    public SalesRankController(SalesRankService salesRankService) {
        this.salesRankService = salesRankService;
    }

    @GetMapping("/sales-rank")
    public Result<List<SalesRankVO>> getSalesRank(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit) {
        List<SalesRankVO> rank = salesRankService.getSalesRank(type, limit);
        return Result.success(rank);
    }
}
