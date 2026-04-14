package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.OrderCreateDTO;
import com.example.spring_test.dto.OrderQueryDTO;
import com.example.spring_test.dto.OrderStatusUpdateDTO;
import com.example.spring_test.dto.ShipOrderDTO;
import com.example.spring_test.entity.Farmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.OrderLogistics;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductImage;
import com.example.spring_test.entity.UserAddress;
import com.example.spring_test.enums.OrderStatusEnum;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.exception.ForbiddenException;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrderLogisticsMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductImageMapper;
import com.example.spring_test.service.AggregateQueryService;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.OrderService;
import com.example.spring_test.util.ImageUtil;
import com.example.spring_test.vo.OrderDetailVO;
import com.example.spring_test.vo.OrderItemVO;
import com.example.spring_test.vo.OrderListVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogisticsMapper orderLogisticsMapper;
    private final ProductImageMapper productImageMapper;
    private final AggregateQueryService aggregateQueryService;
    private final ImageUtil imageUtil;

    public OrderServiceImpl(OrdersMapper ordersMapper,
                            OrderItemMapper orderItemMapper,
                            OrderLogisticsMapper orderLogisticsMapper,
                            ProductImageMapper productImageMapper,
                            AggregateQueryService aggregateQueryService,
                            ImageUtil imageUtil) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderLogisticsMapper = orderLogisticsMapper;
        this.productImageMapper = productImageMapper;
        this.aggregateQueryService = aggregateQueryService;
        this.imageUtil = imageUtil;
    }

    @Override
    @Transactional
    public Long create(OrderCreateDTO orderCreateDTO) {
        log.info("=== 创建订单开始 ===");
        log.info("接收到的订单数据: productId={}, quantity={}, addressId={}, remark={}", 
            orderCreateDTO.getProductId(), 
            orderCreateDTO.getQuantity(), 
            orderCreateDTO.getAddressId(), 
            orderCreateDTO.getRemark());
        log.info("商品列表: {}", orderCreateDTO.getItems());
        
        List<OrderCreateDTO.OrderItemDTO> items = orderCreateDTO.getItems();
        if (items == null || items.isEmpty()) {
            log.info("商品列表为空，使用单个商品模式");
            if (orderCreateDTO.getProductId() != null) {
                items = List.of(new OrderCreateDTO.OrderItemDTO() {{
                    setProductId(orderCreateDTO.getProductId());
                    setQuantity(orderCreateDTO.getQuantity() != null && orderCreateDTO.getQuantity() > 0 
                        ? orderCreateDTO.getQuantity() : 1);
                }});
                log.info("创建单个商品项: productId={}, quantity={}", 
                    orderCreateDTO.getProductId(), 
                    orderCreateDTO.getQuantity());
            } else {
                log.error("商品不能为空");
                throw new BusinessException("商品不能为空");
            }
        }
        
        if (orderCreateDTO.getAddressId() == null) {
            log.error("收货地址ID为空");
            throw new BusinessException("收货地址不能为空");
        }
        
        log.info("查询收货地址: addressId={}", orderCreateDTO.getAddressId());
        UserAddress userAddress = aggregateQueryService.getAddressById(orderCreateDTO.getAddressId());
        if (userAddress == null || !CurrentUserUtil.currentUserId().equals(userAddress.getUserId())) {
            log.error("收货地址不存在或不属于当前用户: addressId={}, userId={}", 
                orderCreateDTO.getAddressId(), 
                CurrentUserUtil.currentUserId());
            throw new BusinessException("收货地址不存在");
        }
        log.info("收货地址验证通过: {}", userAddress.getName());
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> productQuantities = new java.util.HashMap<>();
        Long farmerId = null;
        
        log.info("开始处理商品列表，共 {} 个商品", items.size());
        for (OrderCreateDTO.OrderItemDTO item : items) {
            if (item.getProductId() == null) {
                log.error("商品ID为空");
                throw new BusinessException("商品ID不能为空");
            }
            Integer quantity = item.getQuantity();
            if (quantity == null || quantity <= 0) {
                log.error("购买数量无效: {}", quantity);
                throw new BusinessException("购买数量必须大于0");
            }
            
            log.info("查询商品: productId={}", item.getProductId());
            Product product = aggregateQueryService.getProductById(item.getProductId());
            if (product == null) {
                log.error("商品不存在: productId={}", item.getProductId());
                throw new BusinessException("商品不存在: " + item.getProductId());
            }
            
            if (farmerId == null) {
                farmerId = product.getFarmerId();
                log.info("设置农户ID: farmerId={}", farmerId);
            }
            
            if (product.getSaleStatus() == null || product.getSaleStatus() != 1) {
                log.error("商品已下架: productId={}, productName={}, saleStatus={}", 
                    product.getId(), product.getProductName(), product.getSaleStatus());
                throw new BusinessException("商品已下架: " + product.getProductName());
            }
            if (product.getStock() == null || product.getStock() < quantity) {
                log.error("商品库存不足: productId={}, productName={}, stock={}, quantity={}", 
                    product.getId(), product.getProductName(), product.getStock(), quantity);
                throw new BusinessException("商品库存不足: " + product.getProductName());
            }
            
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subtotal);
            log.info("商品处理成功: productName={}, price={}, quantity={}, subtotal={}", 
                product.getProductName(), product.getPrice(), quantity, subtotal);
            
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getProductName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setSubtotalAmount(subtotal);
            orderItems.add(orderItem);
            
            productQuantities.put(product.getId(), quantity);
        }
        
        log.info("商品处理完成，总金额: {}", totalAmount);
        
        Orders order = new Orders();
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setUserId(CurrentUserUtil.currentUserId());
        order.setFarmerId(farmerId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setOrderStatus(0);
        order.setPayStatus(0);
        order.setReceiverName(userAddress.getName());
        order.setReceiverPhone(userAddress.getPhone());
        order.setReceiverAddress(buildAddress(userAddress));
        order.setRemark(orderCreateDTO.getRemark());
        
        log.info("创建订单: orderNo={}, userId={}, farmerId={}, totalAmount={}", 
            order.getOrderNo(), order.getUserId(), order.getFarmerId(), order.getTotalAmount());
        ordersMapper.insert(order);
        log.info("订单创建成功: orderId={}", order.getId());
        
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemMapper.insert(orderItem);
        }
        log.info("订单明细创建完成，共 {} 条", orderItems.size());
        
        log.info("开始扣减库存");
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            boolean success = aggregateQueryService.decreaseProductStockAndIncreaseSales(productId, quantity);
            
            if (!success) {
                log.error("库存扣减失败: productId={}, quantity={}", productId, quantity);
                throw new BusinessException("库存扣减失败，请重试");
            }
            log.info("库存扣减成功: productId={}, quantity={}", productId, quantity);
        }
        log.info("商品库存更新完成");
        
        log.info("=== 创建订单结束，orderId={} ===", order.getId());
        return order.getId();
    }

    private String buildAddress(UserAddress address) {
        String province = address.getProvince() == null ? "" : address.getProvince();
        String city = address.getCity() == null ? "" : address.getCity();
        String district = address.getDistrict() == null ? "" : address.getDistrict();
        String detail = address.getDetail() == null ? "" : address.getDetail();
        return province + city + district + detail;
    }

    @Override
    public PageResult<OrderListVO> page(OrderQueryDTO orderQueryDTO) {
        // 处理页码和每页大小
        if (orderQueryDTO.getPage() != null && orderQueryDTO.getPage() > 0) {
            orderQueryDTO.setPageNum(orderQueryDTO.getPage());
        }
        
        // 处理状态参数
        if (orderQueryDTO.getStatus() != null) {
            orderQueryDTO.setOrderStatus(orderQueryDTO.getStatus());
        }
        
        // 构建分页
        Page<Orders> page = new Page<>(orderQueryDTO.getPageNum(), orderQueryDTO.getPageSize());
        
        // 解析农户ID（优先使用merchantId，然后是farmerId，最后根据用户类型）
        Long farmerId = resolveFarmerId(orderQueryDTO.getMerchantId() != null ? orderQueryDTO.getMerchantId() : orderQueryDTO.getFarmerId());
        Long userId = resolveUserId();
        
        // 构建查询条件
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<Orders>()
                .like(orderQueryDTO.getOrderNo() != null && !orderQueryDTO.getOrderNo().isBlank(), Orders::getOrderNo, orderQueryDTO.getOrderNo())
                .eq(farmerId != null, Orders::getFarmerId, farmerId)
                .eq(userId != null, Orders::getUserId, userId)
                .eq(orderQueryDTO.getOrderStatus() != null, Orders::getOrderStatus, orderQueryDTO.getOrderStatus())
                .orderByDesc(Orders::getId);
        
        // 执行查询
        Page<Orders> result = ordersMapper.selectPage(page, queryWrapper);
        
        // 构建响应数据
        List<OrderListVO> orderList = buildOrderList(result.getRecords());
        
        // 构建返回结果
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), orderList);
    }

    @Override
    @Cacheable(value = "orderDetail", key = "#id", unless = "#result == null")
    public OrderDetailVO detail(Long id) {
        Orders order = getReadableOrder(id);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orderDetail", key = "#id")
    public OrderDetailVO updateStatus(Long id, OrderStatusUpdateDTO orderStatusUpdateDTO) {
        Long currentUserId = CurrentUserUtil.currentUserId();
        String currentUserType = CurrentUserUtil.getCurrentUserType();
        log.info("用户 [{}] (类型: {}) 尝试更新订单 [{}] 状态", currentUserId, currentUserType, id);
        
        Orders order = getManageableOrder(id);
        Integer targetStatus = orderStatusUpdateDTO.getTargetStatus() != null
                ? orderStatusUpdateDTO.getTargetStatus()
                : orderStatusUpdateDTO.getStatus();
        if (targetStatus == null) {
            throw new BusinessException("目标状态不能为空");
        }
        if (targetStatus.equals(OrderStatusEnum.CANCELED.getCode())) {
            return cancel(id, orderStatusUpdateDTO.getRemark());
        }
        validateStatusTransition(order.getOrderStatus(), targetStatus);
        
        // 使用乐观锁更新订单状态
        int updatedRows = 0;
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            // 先保存旧状态
            Integer oldStatus = order.getOrderStatus();
            LocalDateTime oldUpdateTime = order.getUpdateTime();
            
            // 设置新状态
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
            
            // 使用乐观锁更新
            LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Orders::getId, id)
                        .eq(Orders::getOrderStatus, oldStatus) // 用旧状态
                        .eq(Orders::getUpdateTime, oldUpdateTime); // 用旧更新时间
            
            updatedRows = ordersMapper.update(order, updateWrapper);
            
            if (updatedRows > 0) {
                log.info("订单 [{}] 状态更新成功: {} -> {}", id, oldStatus, targetStatus);
                break;
            }
            
            retryCount++;
            log.warn("订单 [{}] 状态更新失败，可能被其他用户修改，正在重试 ({}/{}) ", id, retryCount, maxRetries);
            
            // 重新获取订单信息
            order = ordersMapper.selectById(id);
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
        }
        
        if (updatedRows == 0) {
            log.error("订单 [{}] 状态更新失败，已达到最大重试次数", id);
            throw new BusinessException("订单状态已被其他用户修改，请刷新后重试");
        }
        
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orderDetail", key = "#id")
    public OrderDetailVO ship(Long id, ShipOrderDTO shipOrderDTO) {
        Long currentUserId = CurrentUserUtil.currentUserId();
        String currentUserType = CurrentUserUtil.getCurrentUserType();
        log.info("用户 [{}] (类型: {}) 尝试发货订单 [{}]", currentUserId, currentUserType, id);
        
        Orders order = getManageableOrder(id);
        if (!Integer.valueOf(OrderStatusEnum.PAID.getCode()).equals(order.getOrderStatus())) {
            log.warn("订单 [{}] 当前状态无法发货: {}", id, order.getOrderStatus());
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
        log.info("订单 [{}] 发货成功，物流: {}, 单号: {}", id, shipOrderDTO.getCompanyName(), shipOrderDTO.getTrackingNo());

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
    @CacheEvict(value = "orderDetail", key = "#id")
    public OrderDetailVO cancel(Long id, String remark) {
        Long currentUserId = CurrentUserUtil.currentUserId();
        String currentUserType = CurrentUserUtil.getCurrentUserType();
        log.info("用户 [{}] (类型: {}) 尝试取消订单 [{}]", currentUserId, currentUserType, id);
        
        Orders order = getReadableOrder(id);
        if (Integer.valueOf(OrderStatusEnum.CANCELED.getCode()).equals(order.getOrderStatus())) {
            log.info("订单 [{}] 已经是取消状态，无需操作", id);
            return buildOrderDetail(order);
        }
        if (Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus())) {
            log.warn("订单 [{}] 已完成，无法取消", id);
            throw new BusinessException("已完成订单不能取消");
        }
        if (Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())) {
            log.warn("订单 [{}] 已发货，无法直接取消", id);
            throw new BusinessException("已发货订单不能直接取消");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id));
        for (OrderItem item : items) {
            aggregateQueryService.increaseProductStockAndDecreaseSales(item.getProductId(), item.getQuantity());
        }
        order.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        order.setCancelTime(LocalDateTime.now());
        if (remark != null && !remark.isBlank()) {
            order.setRemark(remark);
        }
        ordersMapper.updateById(order);
        log.info("订单 [{}] 取消成功", id);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orderDetail", key = "#id")
    public void delete(Long id) {
        Long currentUserId = CurrentUserUtil.currentUserId();
        String currentUserType = CurrentUserUtil.getCurrentUserType();
        log.info("用户 [{}] (类型: {}) 尝试删除订单 [{}]", currentUserId, currentUserType, id);
        
        Orders order = getReadableOrder(id);
        if (!Integer.valueOf(OrderStatusEnum.CANCELED.getCode()).equals(order.getOrderStatus())) {
            log.warn("订单 [{}] 状态不是已取消，无法删除: {}", id, order.getOrderStatus());
            throw new BusinessException("只能删除已取消的订单");
        }
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id));
        orderLogisticsMapper.delete(new LambdaQueryWrapper<OrderLogistics>()
                .eq(OrderLogistics::getOrderId, id));
        ordersMapper.deleteById(id);
        log.info("订单 [{}] 删除成功", id);
    }

    private OrderDetailVO buildOrderDetail(Orders order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        
        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : orderItems) {
            OrderItemVO vo = new OrderItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setProductName(item.getProductName());
            vo.setProductPrice(item.getProductPrice());
            vo.setQuantity(item.getQuantity());
            vo.setSubtotalAmount(item.getSubtotalAmount());
            
            Product product = aggregateQueryService.getProductById(item.getProductId());
            if (product != null) {
                String imageUrl = resolveProductImageUrl(product);
                vo.setProductImage(imageUtil.getImageUrl(imageUrl));
                vo.setProductUnit(product.getUnitName());
                vo.setProductOrigin(product.getOriginPlace());
            }
            
            itemVOs.add(vo);
        }
        
        OrderLogistics logistics = orderLogisticsMapper.selectOne(new LambdaQueryWrapper<OrderLogistics>()
                .eq(OrderLogistics::getOrderId, order.getId())
                .last("limit 1"));
        Farmer farmer = aggregateQueryService.getFarmerById(order.getFarmerId());
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
        vo.setItems(itemVOs);
        if (logistics != null) {
            vo.setLogisticsCompany(logistics.getCompanyName());
            vo.setTrackingNo(logistics.getTrackingNo());
            vo.setLogisticsStatus(logistics.getLogisticsStatus());
            vo.setShipRemark(logistics.getShipRemark());
        }
        return vo;
    }

    private Orders getExistingOrder(Long id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Orders getReadableOrder(Long id) {
        Orders order = getExistingOrder(id);
        SessionUser currentUser = SessionUserHolder.get();
        if (currentUser == null) {
            throw new ForbiddenException("用户未登录");
        }
        Long currentUserId = currentUser.getUserId();
        String currentUserType = currentUser.getRoleCode();
        
        if (isAdmin(currentUser)) {
            log.info("管理员 [{}] 访问订单 [{}]", currentUserId, id);
            return order;
        }
        
        if (isFarmer(currentUser) && currentUserId.equals(order.getFarmerId())) {
            log.info("农户 [{}] 访问自己的订单 [{}]", currentUserId, id);
            return order;
        }
        
        if (isConsumer(currentUser) && currentUserId.equals(order.getUserId())) {
            log.info("消费者 [{}] 访问自己的订单 [{}]", currentUserId, id);
            return order;
        }
        
        log.warn("权限不足: 用户 [{}] (类型: {}) 尝试访问订单 [{}] (用户: {}, 农户: {})", 
            currentUserId, currentUserType, id, order.getUserId(), order.getFarmerId());
        throw new ForbiddenException("您没有操作该订单的权限");
    }

    private Orders getManageableOrder(Long id) {
        Orders order = getExistingOrder(id);
        SessionUser currentUser = SessionUserHolder.get();
        if (currentUser == null) {
            throw new ForbiddenException("用户未登录");
        }
        Long currentUserId = currentUser.getUserId();
        String currentUserType = currentUser.getRoleCode();
        
        if (isAdmin(currentUser)) {
            log.info("管理员 [{}] 操作订单 [{}]", currentUserId, id);
            return order;
        }
        
        if (isFarmer(currentUser) && currentUserId.equals(order.getFarmerId())) {
            log.info("农户 [{}] 操作自己的订单 [{}]", currentUserId, id);
            return order;
        }
        
        if (isConsumer(currentUser) && currentUserId.equals(order.getUserId())) {
            log.info("消费者 [{}] 操作自己的订单 [{}]", currentUserId, id);
            return order;
        }
        
        log.warn("权限不足: 用户 [{}] (类型: {}) 尝试操作订单 [{}] (用户: {}, 农户: {})", 
            currentUserId, currentUserType, id, order.getUserId(), order.getFarmerId());
        throw new ForbiddenException("您没有操作该订单的权限");
    }

    private Long resolveFarmerId(Long queryFarmerId) {
        SessionUser currentUser = SessionUserHolder.get();
        if (isFarmer(currentUser)) {
            return currentUser.getUserId();
        }
        if (isConsumer(currentUser)) {
            return null;
        }
        return queryFarmerId;
    }

    private Long resolveUserId() {
        SessionUser currentUser = SessionUserHolder.get();
        if (isConsumer(currentUser)) {
            return currentUser.getUserId();
        }
        return null;
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
        Map<Long, String> farmerNameMap = farmerIds.isEmpty() ? Collections.emptyMap()
            : aggregateQueryService.getFarmerNameMap(farmerIds);

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
        vo.setFarmerName(farmerNameMap.getOrDefault(order.getFarmerId(), "-"));
        vo.setItemCount(items == null ? 0 : items.stream().mapToInt(OrderItem::getQuantity).sum());
        vo.setItemSummary(buildItemSummary(items));
        
        if (items != null && !items.isEmpty()) {
            List<OrderItemVO> itemVOs = new ArrayList<>();
            for (OrderItem item : items) {
                OrderItemVO itemVO = new OrderItemVO();
                itemVO.setId(item.getId());
                itemVO.setProductId(item.getProductId());
                itemVO.setProductName(item.getProductName());
                itemVO.setProductPrice(item.getProductPrice());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setSubtotalAmount(item.getSubtotalAmount());
                
                Product product = aggregateQueryService.getProductById(item.getProductId());
                if (product != null) {
                    String imageUrl = resolveProductImageUrl(product);
                    itemVO.setProductImage(imageUtil.getImageUrl(imageUrl));
                    itemVO.setProductUnit(product.getUnitName());
                    itemVO.setProductOrigin(product.getOriginPlace());
                }
                
                itemVOs.add(itemVO);
            }
            vo.setItems(itemVOs);
        }
        
        if (logistics != null) {
            vo.setLogisticsCompany(logistics.getCompanyName());
            vo.setTrackingNo(logistics.getTrackingNo());
        }
        
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

    private String resolveProductImageUrl(Product product) {
        String imageUrl = product.getCoverImage();
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        ProductImage productImage = productImageMapper.selectOne(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, product.getId())
                .orderByAsc(ProductImage::getSortOrder)
                .last("limit 1"));
        return productImage == null ? null : productImage.getImageUrl();
    }

    private boolean isAdmin(SessionUser user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean isFarmer(SessionUser user) {
        return user != null && "FARMER".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean isConsumer(SessionUser user) {
        return user != null && "CONSUMER".equalsIgnoreCase(user.getRoleCode());
    }
}