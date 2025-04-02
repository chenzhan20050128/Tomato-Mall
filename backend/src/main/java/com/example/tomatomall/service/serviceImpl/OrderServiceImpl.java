package com.example.tomatomall.service.serviceImpl;/*
 * @date 04/02 19:52
 */

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.easysdk.factory.Factory;
import com.example.tomatomall.config.AliPayConfig;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.dao.CartItemRepository;
import com.example.tomatomall.dao.OrderRepository;
import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.vo.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Resource
    private OrderRepository orderRepository;

    @Resource
    private CartItemRepository cartRepository;

    @Resource
    private ProductRepository productRepository;



    @Resource
    private AliPayConfig aliPayConfig;

    private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do"; // 支付宝沙箱环境地址
    private static final String FORMAT = "JSON";
    private static final String CHARSET = "utf-8";
    private static final String SIGN_TYPE = "RSA2";

    @Override
    @Transactional
    public Order createOrder(Long userId, List<Long> cartItemIds, Object shippingAddress, String paymentMethod) {
        // 获取购物车商品
        List<CartItem> cartItems = cartRepository.findAllByCartItemIdIn(cartItemIds);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("未找到购物车商品");
        }

        // 计算总金额并检查库存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品未找到: " + cartItem.getProductId()));

            // 检查库存
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("商品库存不足: " + product.getTitle());
            }

            // 计算价格
            BigDecimal itemPrice = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);
        }

        // 创建订单
        Orders order = new Orders();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("PENDING"); // 待支付状态

        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单未找到"));
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Orders order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public Map<String, Object> generatePayment(Long orderId) throws Exception {
        Orders order = getOrderById(orderId);

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
        request.setBizContent("{\"out_trade_no\":\"" + order.getOrderId() + "\","
                + "\"total_amount\":\"" + order.getTotalAmount() + "\","
                + "\"subject\":\"Tomato Mall Order #" + order.getOrderId() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String form = "";
        try {
            form = alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new RuntimeException("生成支付表单失败", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paymentForm", form);
        result.put("orderId", order.getOrderId());
        result.put("totalAmount", order.getTotalAmount());
        result.put("paymentMethod", order.getPaymentMethod());

        return result;
    }

    @Override
    @Transactional
    public boolean processPaymentCallback(Map<String, String> params) throws Exception {
        // 验证支付签名
        boolean signVerified = Factory.Payment.Common().verifyNotify(params);

        if (signVerified && "TRADE_SUCCESS".equals(params.get("trade_status"))) {
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String gmtPayment = params.get("gmt_payment");

            // 更新订单状态
            Orders order = getOrderById(Long.valueOf(outTradeNo));
            order.setStatus("SUCCESS"); // 成功状态
            order.setTradeNo(tradeNo);
            order.setPaymentTime(Timestamp.valueOf(gmtPayment.replace("T", " ").substring(0, 19)));
            orderRepository.save(order);

            // 更新商品库存
            List<Cart> cartItems = cartRepository.findByUserId(order.getUserId());
            for (Cart cartItem : cartItems) {
                Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
                if (product != null) {
                    product.setStock(product.getStock() - cartItem.getQuantity());
                    productRepository.save(product);
                }
            }

            return true;
        }

        return false;
    }
}