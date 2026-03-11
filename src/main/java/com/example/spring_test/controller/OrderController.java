package com.example.spring_test.controller;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.common.Result;
import com.example.spring_test.dto.OrderQueryDTO;
import com.example.spring_test.dto.OrderStatusUpdateDTO;
import com.example.spring_test.dto.ShipOrderDTO;
import com.example.spring_test.service.OrderService;
import com.example.spring_test.vo.OrderDetailVO;
import com.example.spring_test.vo.OrderListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Result<PageResult<OrderListVO>> page(OrderQueryDTO orderQueryDTO) {
        return Result.success(orderService.page(orderQueryDTO));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @PutMapping("/{id}/status")
    public Result<OrderDetailVO> updateStatus(@PathVariable Long id, @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO) {
        return Result.success("更新订单状态成功", orderService.updateStatus(id, orderStatusUpdateDTO));
    }

    @PutMapping("/{id}/ship")
    public Result<OrderDetailVO> ship(@PathVariable Long id, @RequestBody ShipOrderDTO shipOrderDTO) {
        return Result.success("订单发货成功", orderService.ship(id, shipOrderDTO));
    }

    @PostMapping("/{id}/cancel")
    public Result<OrderDetailVO> cancel(@PathVariable Long id, @RequestParam(required = false) String remark) {
        return Result.success("取消订单成功", orderService.cancel(id, remark));
    }
}