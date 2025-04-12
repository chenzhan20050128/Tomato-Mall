package com.example.tomatomall.config;/*
 * @date 04/10 11:31
 */

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${mq-config.order-delay-queue}")
    private String orderDelayQueue;
    @Value("${mq-config.order-dead-queue}")
    private String orderDeadQueue;
    @Value("${mq-config.payment-queue}")
    private String paymentQueue;

    // 订单延迟交换机（使用插件）
    @Bean
    public CustomExchange orderDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("order.delay.exchange",
                "x-delayed-message",
                true,
                false,
                args);
    }

    // 支付结果交换机
    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange("payment.exchange");
    }

    // 订单延迟队列
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(orderDelayQueue)
                .deadLetterExchange("order.dead.exchange")
                .deadLetterRoutingKey("order.dead")
                .build();
    }

    // 订单死信队列
    @Bean
    public Queue orderDeadQueue() {
        return new Queue(orderDeadQueue);
    }

    // 支付处理队列
    @Bean
    public Queue paymentQueue() {
        return new Queue(paymentQueue);
    }

    // 绑定延迟队列到延迟交换机
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with("order.delay")
                .noargs();
    }

    // 绑定支付队列
    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(paymentExchange())
                .with("payment.process");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);  // 确保使用JSON转换器
        template.setUseDirectReplyToContainer(false);        // 避免类型转换问题
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);   // 监听器使用JSON转换器
        factory.setDefaultRequeueRejected(false);            // 拒绝消息时不重新入队
        return factory;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost("192.168.56.10");
        factory.setChannelCacheSize(25); // 增加通道缓存
        factory.setChannelCheckoutTimeout(1000); // 设置获取通道超时时间
        return factory;
    }
}