package com.example.tomatomall.consumer;/*
 * @date 04/10 11:38
 */

import com.example.tomatomall.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.io.IOException;
import com.rabbitmq.client.Channel;
@Component
@Slf4j
public class OrderTimeoutConsumer {

    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = "${mq-config.order-delay-queue}")
    public void handleTimeoutOrder(@Payload Map<String, Object> message,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            Integer orderId = (Integer) message.get("orderId");
            log.info("收到订单超时消息，订单ID：{}", orderId);

            // 处理订单超时
            orderService.handleExpiredOrder(orderId);

            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("订单超时处理失败：{}", e.getMessage());
            // 重试3次后进入死信队列
            try {
                channel.basicNack(tag, false, false);
            } catch (IOException ex) {
                log.error("消息NACK失败：{}", ex.getMessage());
            }
        }
    }
}