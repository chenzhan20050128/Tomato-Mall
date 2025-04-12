package com.example.tomatomall.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.easysdk.factory.Factory;
import com.example.tomatomall.config.AliPayConfig;
import com.example.tomatomall.dto.PaymentNotifyDTO;
import com.example.tomatomall.po.AliPay;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/{orderId}/pay")
    public void pay(@PathVariable Integer orderId, HttpServletResponse httpResponse) throws Exception {
        Order order = orderService.getOrderById(orderId);

        if (order.getLockExpireTime().before(new java.util.Date())) {
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

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        log.info("支付宝支付回调地址: {}", aliPayConfig.getNotifyUrl());
        request.setReturnUrl(alipayReturnUrl);//前端返回地址

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderId());  // 我们自己生成的订单编号
        bizContent.put("total_amount", order.getTotalAmount()); // 订单的总金额
        bizContent.put("subject", "Tomato Mall Order #" + orderId);   // 支付的名称
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");  // 固定配置
        request.setBizContent(bizContent.toString());
        log.info(bizContent.toString());

        String form = alipayClient.pageExecute(request).getBody();

        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form);
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
        log.info("支付请求已发送，订单ID: {}", orderId);
        log.info("支付请求表单: {}", form);
        // 这里可以将表单返回给前端，前端可以直接跳转到支付宝支付页面

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
        PaymentNotifyDTO notifyDTO = new PaymentNotifyDTO();
        notifyDTO.setOutTradeNo(params.get("out_trade_no"));
        notifyDTO.setTradeNo(params.get("trade_no"));
        notifyDTO.setTotalAmount(new BigDecimal(params.get("total_amount")));
        Message message = rabbitTemplate.getMessageConverter().toMessage(
                notifyDTO,
                new MessageProperties()
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
}