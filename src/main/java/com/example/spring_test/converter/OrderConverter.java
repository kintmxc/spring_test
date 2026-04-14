package com.example.spring_test.converter;

import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.OrderLogistics;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.vo.OrderDetailVO;
import com.example.spring_test.vo.OrderItemVO;
import com.example.spring_test.vo.OrderListVO;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderConverter {

    public OrderDetailVO toDetailVO(Orders order, List<OrderItemVO> items, 
                                     OrderLogistics logistics, String farmerName) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setFarmerId(order.getFarmerId());
        vo.setFarmerName(farmerName == null ? "-" : farmerName);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusText(toStatusText(order.getOrderStatus()));
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

    public OrderListVO toListVO(Orders order, String farmerName, 
                                 List<OrderItem> items, OrderLogistics logistics) {
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
        vo.setFarmerName(farmerName == null ? "-" : farmerName);
        vo.setItemCount(items == null ? 0 : items.stream().mapToInt(OrderItem::getQuantity).sum());
        vo.setItemSummary(buildItemSummary(items));
        vo.setLogisticsCompany(logistics == null ? null : logistics.getCompanyName());
        vo.setTrackingNo(logistics == null ? null : logistics.getTrackingNo());
        return vo;
    }

    public String toStatusText(Integer status) {
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
}
