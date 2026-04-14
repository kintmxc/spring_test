package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {
    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        return Result.success("后端服务运行中", Map.of(
                "app", "spring_test",
                "status", "UP",
                "apiPrefix", "/api",
                "tips", "请通过前端管理端访问页面，或调用 /api 下接口"
        ));
    }

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "UP"));
    }
}