package com.example.spring_test.service;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.OrderCreateDTO;
import com.example.spring_test.dto.OrderQueryDTO;
import com.example.spring_test.dto.OrderStatusUpdateDTO;
import com.example.spring_test.dto.ShipOrderDTO;
import com.example.spring_test.vo.OrderDetailVO;
import com.example.spring_test.vo.OrderListVO;

public interface OrderService {
    Long create(OrderCreateDTO orderCreateDTO);

    PageResult<OrderListVO> page(OrderQueryDTO orderQueryDTO);

    OrderDetailVO detail(Long id);

    OrderDetailVO updateStatus(Long id, OrderStatusUpdateDTO orderStatusUpdateDTO);

    OrderDetailVO ship(Long id, ShipOrderDTO shipOrderDTO);

    OrderDetailVO cancel(Long id, String remark);

    void delete(Long id);
}