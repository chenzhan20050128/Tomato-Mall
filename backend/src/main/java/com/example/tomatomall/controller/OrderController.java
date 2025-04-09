package com.example.tomatomall.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.easysdk.factory.Factory;
import com.example.tomatomall.config.AliPayConfig;
import com.example.tomatomall.po.AliPay;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class OrderController {

    @Resource
    AliPayConfig aliPayConfig;

    private static final String GATEWAY_URL ="https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String FORMAT ="JSON";
    private static final String CHARSET ="utf-8";
    private static final String SIGN_TYPE ="RSA2";

    @Autowired
    private OrderService orderService;

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
        request.setReturnUrl("http://baidu.com");//前端返回地址

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderId());  // 我们自己生成的订单编号
        bizContent.put("total_amount", order.getTotalAmount()); // 订单的总金额
        bizContent.put("subject", "Tomato Mall Order #" + orderId);   // 支付的名称
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");  // 固定配置
        request.setBizContent(bizContent.toString());

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
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

        try {
            // 1. 签名验证
            boolean verifyResult = Factory.Payment.Common().verifyNotify(params);
            if (!verifyResult) {
                response.getWriter().print("fail");
                return;
            }

            // 2. 状态校验
            if (!"TRADE_SUCCESS".equals(params.get("trade_status")) && !"TRADE_FINISHED".equals(params.get("trade_status"))) {
                response.getWriter().print("success");
                return;
            }

            // 3. 业务处理
            String orderId = params.get("out_trade_no");
            Order order = orderService.getOrderById(Integer.parseInt(orderId));

            // 幂等性检查（防止重复处理）
            if ("SUCCESS".equals(order.getStatus())) {
                response.getWriter().print("success");
                return;
            }

            // 4. 处理支付结果
            boolean success = orderService.processPaymentCallback(params);
            response.getWriter().print(success ? "success" : "fail");
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().print("fail");
            log.error("支付回调处理失败", e);
        }
    }

    @GetMapping("/{orderId}/status")
    public Response<Order> checkOrderStatus(@PathVariable Integer orderId) {
        return Response.buildSuccess(orderService.getOrderById(orderId));
    }
}