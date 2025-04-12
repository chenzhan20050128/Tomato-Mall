package com.example.tomatomall.consumer;/*
 * @date 04/10 11:49
 */

import com.example.tomatomall.dto.PaymentNotifyDTO;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
@Component
@Slf4j
public class PaymentResultConsumer {

    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = "${mq-config.payment-queue}")
    public void handlePaymentResult(@Payload PaymentNotifyDTO notifyDTO,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.info("收到支付结果消息: {}", notifyDTO);
        try {
            // 3. 业务处理
            Order order = orderService.getOrderById(
                    Integer.parseInt(notifyDTO.getOutTradeNo()));
            log.info("查询到订单信息: {} {}", order.getOrderId(),order.getTotalAmount());

            // 幂等性检查（防止重复处理）
            log.info("开始幂等性检查，当前订单状态: {}", order.getStatus());
            if ("SUCCESS".equals(order.getStatus())) {
                log.info("订单已处理过，直接返回success");
                return;
            }

            // 4. 处理支付结果
            log.info("开始处理支付结果");
            boolean success = orderService.processPaymentCallback(notifyDTO);

            if (success) {
                // 确保通道仍然有效
                if (channel.isOpen()) {
                    channel.basicAck(tag, false);
                    log.info("消息确认成功，tag: {}", tag);
                } else {
                    log.warn("通道已关闭，无法确认消息");
                }
            } else {
                channel.basicNack(tag, false, true);

            }
        } catch (Exception e) {
            log.error("支付回调处理异常: {}", e.getMessage(), e);
            try {
                if (channel.isOpen()) {
                    channel.basicNack(tag, false, false);
                }
            } catch (IOException ex) {
                log.error("消息NACK失败：{}", ex.getMessage());
            }
        } finally {
            // 添加通道状态检查日志
            log.debug("通道状态：isOpen={}", channel.isOpen());
        }

    }


}