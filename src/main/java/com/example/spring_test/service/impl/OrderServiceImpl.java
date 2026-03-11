package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.OrderQueryDTO;
import com.example.spring_test.dto.OrderStatusUpdateDTO;
import com.example.spring_test.dto.ShipOrderDTO;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.OrderLogistics;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.enums.OrderStatusEnum;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrderLogisticsMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.OrderService;
import com.example.spring_test.vo.OrderDetailVO;
import com.example.spring_test.vo.OrderListVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogisticsMapper orderLogisticsMapper;
    private final ProductMapper productMapper;
    private final FarmerMapper farmerMapper;

    public OrderServiceImpl(OrdersMapper ordersMapper,
                            OrderItemMapper orderItemMapper,
                            OrderLogisticsMapper orderLogisticsMapper,
                            ProductMapper productMapper,
                            FarmerMapper farmerMapper) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderLogisticsMapper = orderLogisticsMapper;
        this.productMapper = productMapper;
        this.farmerMapper = farmerMapper;
    }

    @Override
    public PageResult<OrderListVO> page(OrderQueryDTO orderQueryDTO) {
        Page<Orders> page = new Page<>(orderQueryDTO.getPageNum(), orderQueryDTO.getPageSize());
        Long farmerId = resolveFarmerId(orderQueryDTO.getFarmerId());
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<Orders>()
                .like(orderQueryDTO.getOrderNo() != null && !orderQueryDTO.getOrderNo().isBlank(), Orders::getOrderNo, orderQueryDTO.getOrderNo())
            .eq(farmerId != null, Orders::getFarmerId, farmerId)
                .eq(orderQueryDTO.getOrderStatus() != null, Orders::getOrderStatus, orderQueryDTO.getOrderStatus())
                .orderByDesc(Orders::getId);
        Page<Orders> result = ordersMapper.selectPage(page, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), buildOrderList(result.getRecords()));
    }

    @Override
    public OrderDetailVO detail(Long id) {
        Orders order = getOrder(id);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailVO updateStatus(Long id, OrderStatusUpdateDTO orderStatusUpdateDTO) {
        Orders order = getOrder(id);
        Integer targetStatus = orderStatusUpdateDTO.getTargetStatus();
        if (targetStatus == null) {
            throw new BusinessException("目标状态不能为空");
        }
        if (targetStatus.equals(OrderStatusEnum.CANCELED.getCode())) {
            return cancel(id, orderStatusUpdateDTO.getRemark());
        }
        validateStatusTransition(order.getOrderStatus(), targetStatus);
        order.setOrderStatus(targetStatus);
        if (targetStatus.equals(OrderStatusEnum.PAID.getCode())) {
            order.setPayStatus(1);
            order.setPayTime(LocalDateTime.now());
        }
        if (targetStatus.equals(OrderStatusEnum.COMPLETED.getCode())) {
            order.setFinishTime(LocalDateTime.now());
        }
        if (orderStatusUpdateDTO.getRemark() != null && !orderStatusUpdateDTO.getRemark().isBlank()) {
            order.setRemark(orderStatusUpdateDTO.getRemark());
        }
        ordersMapper.updateById(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailVO ship(Long id, ShipOrderDTO shipOrderDTO) {
        Orders order = getOrder(id);
        if (!Integer.valueOf(OrderStatusEnum.PAID.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException("当前订单状态无法执行发货操作");
        }
        if (shipOrderDTO.getCompanyName() == null || shipOrderDTO.getCompanyName().isBlank()) {
            throw new BusinessException("物流公司不能为空");
        }
        if (shipOrderDTO.getTrackingNo() == null || shipOrderDTO.getTrackingNo().isBlank()) {
            throw new BusinessException("物流单号不能为空");
        }
        order.setOrderStatus(OrderStatusEnum.SHIPPED.getCode());
        order.setShipTime(LocalDateTime.now());
        if (shipOrderDTO.getShipRemark() != null && !shipOrderDTO.getShipRemark().isBlank()) {
            order.setRemark(shipOrderDTO.getShipRemark());
        }
        ordersMapper.updateById(order);

        OrderLogistics logistics = orderLogisticsMapper.selectOne(new LambdaQueryWrapper<OrderLogistics>()
                .eq(OrderLogistics::getOrderId, id)
                .last("limit 1"));
        if (logistics == null) {
            logistics = new OrderLogistics();
            logistics.setOrderId(id);
            logistics.setCompanyName(shipOrderDTO.getCompanyName());
            logistics.setTrackingNo(shipOrderDTO.getTrackingNo());
            logistics.setLogisticsStatus(1);
            logistics.setShipRemark(shipOrderDTO.getShipRemark());
            orderLogisticsMapper.insert(logistics);
        } else {
            logistics.setCompanyName(shipOrderDTO.getCompanyName());
            logistics.setTrackingNo(shipOrderDTO.getTrackingNo());
            logistics.setLogisticsStatus(1);
            logistics.setShipRemark(shipOrderDTO.getShipRemark());
            orderLogisticsMapper.updateById(logistics);
        }
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailVO cancel(Long id, String remark) {
        Orders order = getOrder(id);
        if (Integer.valueOf(OrderStatusEnum.CANCELED.getCode()).equals(order.getOrderStatus())) {
            return buildOrderDetail(order);
        }
        if (Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException("已完成订单不能取消");
        }
        if (Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException("已发货订单不能直接取消");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }
        order.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        order.setCancelTime(LocalDateTime.now());
        if (remark != null && !remark.isBlank()) {
            order.setRemark(remark);
        }
        ordersMapper.updateById(order);
        return buildOrderDetail(order);
    }

    private OrderDetailVO buildOrderDetail(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        OrderLogistics logistics = orderLogisticsMapper.selectOne(new LambdaQueryWrapper<OrderLogistics>()
                .eq(OrderLogistics::getOrderId, order.getId())
                .last("limit 1"));
        Farmer farmer = farmerMapper.selectById(order.getFarmerId());
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setFarmerId(order.getFarmerId());
        vo.setFarmerName(farmer == null ? "-" : farmer.getFarmerName());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusText(orderStatusText(order.getOrderStatus()));
        vo.setPayStatus(order.getPayStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setItems(items);
        if (logistics != null) {
            vo.setLogisticsCompany(logistics.getCompanyName());
            vo.setTrackingNo(logistics.getTrackingNo());
            vo.setLogisticsStatus(logistics.getLogisticsStatus());
            vo.setShipRemark(logistics.getShipRemark());
        }
        return vo;
    }

    private Orders getOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        CurrentUserUtil.requireAdminOrOwner(order.getFarmerId(), "无权访问该订单");
        return order;
    }

    private Long resolveFarmerId(Long queryFarmerId) {
        if (CurrentUserUtil.isFarmer()) {
            return CurrentUserUtil.currentUserId();
        }
        return queryFarmerId;
    }

    private void validateStatusTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null) {
            throw new BusinessException("订单状态异常");
        }
        if (currentStatus.equals(targetStatus)) {
            return;
        }
        boolean valid = switch (currentStatus) {
            case 0 -> targetStatus.equals(OrderStatusEnum.PAID.getCode()) || targetStatus.equals(OrderStatusEnum.CANCELED.getCode());
            case 1 -> targetStatus.equals(OrderStatusEnum.SHIPPED.getCode()) || targetStatus.equals(OrderStatusEnum.CANCELED.getCode());
            case 2 -> targetStatus.equals(OrderStatusEnum.COMPLETED.getCode());
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("当前订单状态无法变更到目标状态");
        }
    }

    private List<OrderListVO> buildOrderList(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toSet());
        Set<Long> farmerIds = orders.stream()
                .map(Orders::getFarmerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));
        List<OrderLogistics> logisticsList = orderLogisticsMapper.selectList(new LambdaQueryWrapper<OrderLogistics>()
                .in(OrderLogistics::getOrderId, orderIds)
                .orderByDesc(OrderLogistics::getId));

        Map<Long, List<OrderItem>> itemsByOrderId = orderItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, OrderLogistics> logisticsByOrderId = logisticsList.stream()
                .collect(Collectors.toMap(OrderLogistics::getOrderId, logistics -> logistics, (current, ignored) -> current));
        Map<Long, String> farmerNameMap = farmerIds.isEmpty()
                ? Collections.emptyMap()
                : farmerMapper.selectBatchIds(farmerIds).stream().collect(Collectors.toMap(Farmer::getId, Farmer::getFarmerName));

        List<OrderListVO> records = new ArrayList<>();
        for (Orders order : orders) {
            records.add(toOrderListVO(order, farmerNameMap, itemsByOrderId.get(order.getId()), logisticsByOrderId.get(order.getId())));
        }
        return records;
    }

    private OrderListVO toOrderListVO(Orders order,
                                      Map<Long, String> farmerNameMap,
                                      List<OrderItem> items,
                                      OrderLogistics logistics) {
        OrderListVO vo = new OrderListVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setFarmerId(order.getFarmerId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setFarmerName(farmerNameMap.getOrDefault(order.getFarmerId(), "-"));
        vo.setItemCount(items == null ? 0 : items.stream().mapToInt(OrderItem::getQuantity).sum());
        vo.setItemSummary(buildItemSummary(items));
        vo.setLogisticsCompany(logistics == null ? null : logistics.getCompanyName());
        vo.setTrackingNo(logistics == null ? null : logistics.getTrackingNo());
        return vo;
    }

    private String buildItemSummary(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "-";
        }
        if (items.size() == 1) {
            OrderItem item = items.get(0);
            return item.getProductName() + " x" + item.getQuantity();
        }
        OrderItem firstItem = items.get(0);
        return firstItem.getProductName() + " 等" + items.size() + "种商品";
    }

    private String orderStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case 0 -> "已创建";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知状态";
        };
    }
}