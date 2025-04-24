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

import java.util.Collections;
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
    /*
在你的项目中使用**死信队列（Dead Letter Queue, DLQ）**，是一种非常成熟、稳定且灵活的消息失败兜底机制。你这段 `RabbitMQConfig` 配置中已经体现出死信队列在订单模块中的关键作用，比如：

```java
@Value("${mq-config.order-dead-queue}")
private String orderDeadQueue;

@Bean
public Queue orderDelayQueue() {
    return QueueBuilder.durable(orderDelayQueue)
            .deadLetterExchange("order.dead.exchange")
            .deadLetterRoutingKey("order.dead")
            .build();
}
```

我们下面详细分析一下 **死信队列的作用、原理以及你这个项目中的实际用途。**

---

## 🎯 一、为什么使用死信队列？（动机）

在实际业务中，消息队列中可能会出现：

1. **消息消费失败**（消费者宕机或代码异常）
2. **消息超时未被消费**
3. **队列拒绝消费的消息**（如手动 `reject` 且 `requeue = false`）

为了防止消息“直接丢失”，我们可以把这些“失败消息”路由到 **专门的死信队列**，做后续处理，比如：

- 重试消费
- 警报通知
- 人工介入
- 异步补偿

> ✅ 死信队列就是“消息回收站”，用于捕捉处理失败的消息，让系统更具健壮性和可观测性。

---

## 🔧 二、死信队列的原理

RabbitMQ 中有三种情况会触发“死信”：

| 触发条件                  | 说明                                               |
|---------------------------|----------------------------------------------------|
| 消息被拒绝（basic.reject/basic.nack） | 并且不重新入队 `requeue=false`                   |
| 消息过期（TTL 到期）      | 未被消费的消息在设置的时间内失效                   |
| 队列满了（超出 max length） | 新消息到达时被拒绝                                |

当这些情况发生时，消息就会根据以下参数转发到另一个队列：

- `x-dead-letter-exchange`: 死信交换机
- `x-dead-letter-routing-key`: 死信路由键

---

## 🧱 三、你的项目中死信队列的应用场景分析

```java
@Bean
public Queue orderDelayQueue() {
    return QueueBuilder.durable(orderDelayQueue)
            .deadLetterExchange("order.dead.exchange")
            .deadLetterRoutingKey("order.dead")
            .build();
}
```

你配置了一个订单延迟队列（模拟订单超时未支付），并设置了死信转发。

### ✅ 实际业务含义如下：

- 当用户下单后，系统会发送一条消息到 `orderDelayQueue`，该队列是延迟队列，消息可能设置了 TTL。
- 如果在指定时间内用户 **没有完成支付**，那么这条订单消息将**过期**。
- 过期后的消息会被转发到死信交换机 `order.dead.exchange`，再路由到 `orderDeadQueue`。
- 在 `orderDeadQueue` 中，你可以监听这些“未支付订单”，并执行如下动作：
  - **自动关闭订单**
  - **释放库存**
  - **记录日志或告警**

### 🎯 死信队列作用总结

| 功能                         | 实际应用                                                   |
|------------------------------|------------------------------------------------------------|
| 延迟任务（+ TTL）             | 订单未支付，超时自动取消                                   |
| 异常重试或人工干预            | 消费失败的消息集中处理                                     |
| 系统容错与解耦                | 不让消息直接丢失，增强容错能力                             |
| 数据观测与追踪                | 记录死信队列消息便于排查和追踪问题                         |

---

## 💡 四、实际建议

如果你希望对死信队列更深入控制，可考虑：

1. **监听死信队列，自动处理未支付订单**
2. **记录死信日志，便于系统监控**
3. **将死信消息转移到“重试队列”做自动重试（指数退避）**
4. **统一封装死信队列通用处理器，减少重复代码**

---

## ✅ 总结一句话：

> 在你当前的电商项目中，死信队列用于处理“订单延迟未支付”场景 —— 一旦超时就将订单转发到死信队列，从而触发自动关闭、释放库存等后续业务逻辑，确保系统稳定、数据一致、用户体验良好。

如果你想实现 **死信队列的监听器和关闭订单的业务逻辑**，我也可以继续帮你写出完整的监听类，要不要？

     */


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
        factory.setHost("127.0.0.1");
        factory.setChannelCacheSize(25); // 增加通道缓存
        factory.setChannelCheckoutTimeout(1000); // 设置获取通道超时时间
        return factory;
    }
}