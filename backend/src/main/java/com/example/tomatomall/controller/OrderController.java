package com.example.tomatomall.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.easysdk.factory.Factory;
import com.example.tomatomall.config.AliPayConfig;
import com.example.tomatomall.dto.PaymentNotifyDTO;
import com.example.tomatomall.po.AliPay;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.service.OrderService;
// 在其他import语句后添加:
import com.example.tomatomall.util.JwtTokenUtil;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/orders")
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OrderController {

    @Resource
    AliPayConfig aliPayConfig;

    @Value("${alipay.returnUrl}")
    private String alipayReturnUrl;

    private static final String GATEWAY_URL ="https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String FORMAT ="JSON";
    private static final String CHARSET ="utf-8";
    private static final String SIGN_TYPE ="RSA2";

    // 在类的顶部添加依赖注入
    @Autowired
    private JwtTokenUtil jwtTokenUtil; // 添加此行

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/{orderId}/pay")
    public void pay(@PathVariable Integer orderId, 
                    @RequestParam(value = "token", required = false) String tokenParam,
                    @RequestParam(value = "authorization", required = false) String authParam,
                    HttpServletRequest request,
                    HttpServletResponse response) throws Exception {
        
        // 尝试获取认证信息(多种方式)
        String token = null;
        
        // 1. 从请求头获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        // 2. 从表单authorization参数获取
        else if (authParam != null && authParam.startsWith("Bearer ")) {
            token = authParam.substring(7);
        }
        // 3. 从表单token参数获取
        else if (tokenParam != null) {
            token = tokenParam;
        }
        
        // 验证token并设置用户身份
        if (token != null) {
            try {
                Integer userId = jwtTokenUtil.getUserIdFromToken(token);
                request.setAttribute("userId", userId);
                log.info("从表单参数获取用户认证信息成功, userId: {}", userId);
            } catch (Exception e) {
                log.error("token验证失败", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("认证失败");
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("未提供有效的认证信息");
            return;
        }
        
        // 继续处理支付逻辑...
        Order order = orderService.getOrderById(orderId);
        
        // 添加详细调试日志
        log.info("支付订单信息: ID={}, 状态={}, 创建时间={}, 锁定过期时间={}, 当前时间={}",
                 order.getOrderId(), 
                 order.getStatus(), 
                 order.getCreateTime(),
                 order.getLockExpireTime(), 
                 new Date());
        
        // 检查是否已过期并记录结果
        boolean isExpired = order.getLockExpireTime().before(new Date());
        log.info("订单是否已过期: {}", isExpired);
        
        if (isExpired) {
            throw new RuntimeException("订单已过期，请重新下单");
        }

        AlipayClient alipayClient = new DefaultAlipayClient(
                GATEWAY_URL,
                aliPayConfig.getAppId(),
                aliPayConfig.getAppPrivateKey(),
                FORMAT,
                CHARSET,
                aliPayConfig.getAlipayPublicKey(),
                SIGN_TYPE);


        // 将这段代码修改为使用自动关闭URL
        String returnUrlWithToken = "http://localhost:8080/api/orders/payment-success?orderId=" + orderId;
        if (token != null) {
            returnUrlWithToken += "&token=" + token;
        }     
              
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setNotifyUrl(aliPayConfig.getNotifyUrl());
        log.info("支付宝支付回调地址: {}", aliPayConfig.getNotifyUrl());
        alipayRequest.setReturnUrl(returnUrlWithToken);//前端返回地址
        log.info("支付宝支付返回地址(+token): {}", returnUrlWithToken);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderId());
        bizContent.put("total_amount", order.getTotalAmount());
        bizContent.put("subject", "Tomato Mall Order #" + orderId);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        alipayRequest.setBizContent(bizContent.toString());
        log.info(bizContent.toString());

        String form = alipayClient.pageExecute(alipayRequest).getBody();

        response.setContentType("text/html;charset=" + CHARSET);
        response.getWriter().write(form);
        response.getWriter().flush();
        response.getWriter().close();
        log.info("支付请求已发送，订单ID: {}", orderId);
        log.info("支付请求表单: {}", form);
    }

    

    @PostMapping("/notify")
    public void payNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("支付宝支付回调开始");

        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        log.info("接收到回调参数: {}", params);
        log.info("开始进行签名验证");
        boolean verifyResult = Factory.Payment.Common().verifyNotify(params);
        log.info("签名验证结果: {}", verifyResult);

        if (!verifyResult) {
            log.info("签名验证失败，返回fail");
            response.getWriter().print("fail");
            return;
        }

        String tradeStatus = params.get("trade_status");
        log.info("交易状态: {}", tradeStatus);

        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("交易状态不是成功或完成状态，直接返回success");
            response.getWriter().print("success");
            return;
        }

        // 使用类型安全的传输对象代替原始Map modified by cz on 4.11 at 10:35
        // 并添加消息持久化 at 4.14 17：24
        PaymentNotifyDTO notifyDTO = new PaymentNotifyDTO();
        notifyDTO.setOutTradeNo(params.get("out_trade_no"));
        notifyDTO.setTradeNo(params.get("trade_no"));
        notifyDTO.setTotalAmount(new BigDecimal(params.get("total_amount")));

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);  // 设为持久化
        /*
        消息持久化的执行流程
下面是当你发送一条持久化消息时，RabbitMQ 的内部执行流程：

1. 生产者发送消息（设置了 deliveryMode = PERSISTENT）
RabbitTemplate 会将消息封装成一个 Message 对象。

在 MessageProperties 中添加 deliveryMode = PERSISTENT。

MessageProperties props = new MessageProperties();
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
Message msg = new Message(bytes, props);
2. RabbitMQ 接收到消息后判断：
当前消息是否为 PERSISTENT；

当前队列是否 durable；

当前消息是否被确认成功持久化到磁盘（WAL）。

3. 写入磁盘（WAL 日志 + Mnesia 存储）
RabbitMQ 并不会马上将消息写入队列文件，而是先写入一个称为 Write-Ahead Log (WAL) 的日志结构。

RabbitMQ 使用 Erlang 自带的 Mnesia 数据库 存储元信息，如队列结构、交换机、绑定等。

4. flush 到磁盘
RabbitMQ 通过一定策略（时间、批次、内存阈值等）将 WAL 中的消息同步 flush 到磁盘中的队列文件中，以此保障消息可靠性。
         */

        Message message = rabbitTemplate.getMessageConverter().toMessage(
                notifyDTO,
                messageProperties
        );

        rabbitTemplate.send(
                "payment.exchange",
                "payment.process",
                message
        );

        log.info("rabbitmq发送消息成功convertAndSend params（实际上是PaymentNotifyDTO）: {}", params);


        response.getWriter().print("success");
    }

    @GetMapping("/{orderId}/status")
    public Response<Order> checkOrderStatus(@PathVariable Integer orderId) {
        return Response.buildSuccess(orderService.getOrderById(orderId));
    }


    
    /**
     * 获取当前用户的订单列表
     */
    @GetMapping
    public Response<List<Order>> getUserOrders(@RequestAttribute("userId") Integer userId) {
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            return Response.buildSuccess(orders);
        } catch (Exception e) {
            log.error("获取用户订单失败: {}", e.getMessage(), e);
            return Response.buildFailure("获取订单列表失败: " + e.getMessage(), "500");
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public Response<Order> cancelOrder(
            @PathVariable Integer orderId,
            @RequestAttribute("userId") Integer userId) {
        try {
            Order cancelledOrder = orderService.cancelOrder(orderId, userId);
            return Response.buildSuccess(cancelledOrder);
        } catch (RuntimeException e) {
            log.warn("取消订单失败: {}", e.getMessage());
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            log.error("取消订单发生异常: {}", e.getMessage(), e);
            return Response.buildFailure("系统错误，取消订单失败", "500");
        }
    }

    /**
     * 确认收货
     */
    @PostMapping("/{orderId}/confirm")
    public Response<Order> confirmReceipt(
            @PathVariable Integer orderId,
            @RequestAttribute("userId") Integer userId) {
        try {
            Order confirmedOrder = orderService.confirmReceipt(orderId, userId);
            return Response.buildSuccess(confirmedOrder);
        } catch (RuntimeException e) {
            log.warn("确认收货失败: {}", e.getMessage());
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            log.error("确认收货发生异常: {}", e.getMessage(), e);
            return Response.buildFailure("系统错误，确认收货失败", "500");
        }
    }

    
    @PostMapping("/{orderId}/check-payment")
    public Response<Order> checkPaymentManually(
            @PathVariable Integer orderId,
            @RequestAttribute(value = "userId", required = false) Integer userId) {
        
        try {
            // 获取订单
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return Response.buildFailure("订单不存在", "404");
            }
            
            // 如果有userId则验证权限，没有则略过权限检查
            if (userId != null && !order.getUserId().equals(userId)) {
                return Response.buildFailure("无权访问此订单", "403");
            }
            
            // 已支付订单无需检查
            if (!"PENDING".equals(order.getStatus())) {
                return Response.buildSuccess(order);
            }
            
            // 调用支付宝查询接口检查支付状态
            boolean isPaid = orderService.checkPaymentStatusWithAlipay(orderId);
            
            if (isPaid) {
                // 更新订单状态
                order.setStatus("PAID");
                order.setPaymentTime(new Timestamp(new Date().getTime()));
                order = orderService.updateOrder(order);
                log.info("手动检查订单支付状态：已支付 orderId={}", orderId);
            }
            
            return Response.buildSuccess(order);
        } catch (Exception e) {
            log.error("检查支付状态失败: {}", e.getMessage(), e);
            return Response.buildFailure("检查支付状态失败: " + e.getMessage(), "500");
        }
    }
    
    // 添加支付成功后的自动关闭页面
    @GetMapping("/payment-success")
    public void paymentSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String orderId = request.getParameter("orderId");
        
        // 输出自动关闭页面
        response.setContentType("text/html;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write("<!DOCTYPE html>");
        writer.write("<html lang='zh-CN'>");
        writer.write("<head><meta charset='utf-8'><title>支付成功</title></head>");
        writer.write("<body>");
        writer.write("<div style='text-align:center;padding:40px;'>");
        writer.write("<h2 style='color:#67C23A'>支付已完成！</h2>");
        writer.write("<p>订单号: " + orderId + "</p>");
        writer.write("<p>页面将在<span id='countdown'>5</span>秒后自动关闭...</p>");
        writer.write("</div>");
        writer.write("<script>");
        // 倒计时并通知父窗口
        writer.write("let seconds = 5;");
        writer.write("const timer = setInterval(() => {");
        writer.write("  seconds--;");
        writer.write("  document.getElementById('countdown').textContent = seconds;");
        writer.write("  if (seconds <= 0) {");
        writer.write("    clearInterval(timer);");
        writer.write("    try {");
        writer.write("      window.opener.postMessage({type: 'PAYMENT_COMPLETE', orderId: '" + orderId + "'}, '*');");
        writer.write("    } catch(e) { console.error('无法通知父窗口', e); }");
        writer.write("    window.close();");
        writer.write("  }");
        writer.write("}, 1000);");
        writer.write("</script>");
        writer.write("</body></html>");
        writer.flush();
    }
}