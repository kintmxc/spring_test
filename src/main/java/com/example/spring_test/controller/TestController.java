package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.exception.BusinessException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @RequestMapping("/sayHello")
    public Result<String> sayHello(@RequestParam String name) {
        return Result.success("Hello1 " + name);
    }

    @RequestMapping("/throwError")
    public Result<Void> throwError() {
        throw new BusinessException("测试业务异常处理");
    }
}
