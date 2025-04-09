package com.example.tomatomall.util;/*
 * @date 04/03 17:18
 */

import com.example.tomatomall.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.tomatomall.po.Order;

import java.util.List;


@Component
@Slf4j
public class ScheduledTask {
    @Autowired
    private OrderService orderService;

    @Scheduled(fixedRate = 5 * 60 * 1000) // 每5分钟检查一次
    public void unlockExpiredStock() {
        List<Order> expiredOrders = orderService.findExpiredOrders();
        expiredOrders.forEach(order -> {
            try {
                orderService.handleExpiredOrder(order.getOrderId());
            } catch (Exception e) {
                log.error("处理过期订单失败：{}", order.getOrderId(), e);
            }
        });
    }
}