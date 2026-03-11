package com.example.spring_test.controller;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.common.Result;
import com.example.spring_test.dto.TraceQueryDTO;
import com.example.spring_test.dto.TraceSaveDTO;
import com.example.spring_test.service.TraceService;
import com.example.spring_test.vo.TraceListVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traces")
public class TraceController {
    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/page")
    public Result<PageResult<TraceListVO>> page(TraceQueryDTO traceQueryDTO) {
        return Result.success(traceService.page(traceQueryDTO));
    }

    @GetMapping
    public Result<TraceListVO> getByProductId(@RequestParam Long productId) {
        return Result.success(traceService.getByProductId(productId));
    }

    @PostMapping
    public Result<TraceListVO> saveOrUpdate(@RequestBody TraceSaveDTO traceSaveDTO) {
        return Result.success("保存追溯信息成功", traceService.saveOrUpdate(traceSaveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<TraceListVO> disable(@PathVariable Long id) {
        return Result.success("停用追溯信息成功", traceService.disable(id));
    }
}