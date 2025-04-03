package com.example.tomatomall.service;

import com.example.tomatomall.po.Order;
import com.example.tomatomall.vo.Response;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Order createOrder(Integer userId, List<Integer> cartItemIds, Object shippingAddress, String paymentMethod); // 创建订单
    Order getOrderById(Integer orderId); // 根据ID获取订单
    List<Order> getOrdersByUserId(Integer userId); // 根据用户ID获取订单列表
    Order updateOrderStatus(Integer orderId, String status); // 更新订单状态
    Map<String, Object> generatePayment(Integer orderId) throws Exception;
    // 生成支付信息
    boolean processPaymentCallback(Map<String, String> params) throws Exception;
    // 处理支付回调
}