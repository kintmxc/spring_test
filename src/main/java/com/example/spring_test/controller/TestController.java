package com.example.spring_test.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @RequestMapping("/sayHello")
    public String sayHello(@RequestParam String name) {
        return "Hello1 " + name;
    }
}
