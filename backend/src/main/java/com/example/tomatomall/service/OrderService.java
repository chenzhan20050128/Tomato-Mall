package com.example.tomatomall.service;

import com.example.tomatomall.dto.PaymentNotifyDTO;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.vo.Response;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Order createOrder(Integer userId, List<Integer> cartItemIds, Object shippingAddress, String paymentMethod); // 创建订单
    Order getOrderById(Integer orderId); // 根据ID获取订单

    Order getOrderByTradeNo(String tradeNo);

    List<Order> getOrdersByUserId(Integer userId); // 根据用户ID获取订单列表
    Order updateOrderStatus(Integer orderId, String status); // 更新订单状态
    // 生成支付信息
    boolean processPaymentCallback(PaymentNotifyDTO paymentNotifyDTO) throws Exception;

    @Transactional
    void handleExpiredOrder(Integer orderId);
    // 处理支付回调

    List<Order> findExpiredOrders();

    // 新增方法
    Order cancelOrder(Integer orderId, Integer userId) throws Exception;
    Order confirmReceipt(Integer orderId, Integer userId) throws Exception;

    
    /**
     * 通过支付宝API检查订单支付状态
     * @param orderId 订单ID
     * @return 是否已支付
     */
    boolean checkPaymentStatusWithAlipay(Integer orderId) throws Exception;
    
    /**
     * 更新订单信息
     * @param order 要更新的订单对象
     * @return 更新后的订单
     */
    Order updateOrder(Order order);
}