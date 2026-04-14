package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.EvaluationSaveDTO;
import com.example.spring_test.service.EvaluationService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody EvaluationSaveDTO evaluationSaveDTO) {
        return Result.success("提交评价成功", evaluationService.create(evaluationSaveDTO));
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        if (page != null || pageSize != null) {
            return Result.success("获取评价列表成功", evaluationService.list(productId, page, pageSize));
        }
        return Result.success(evaluationService.list(productId));
    }
}
