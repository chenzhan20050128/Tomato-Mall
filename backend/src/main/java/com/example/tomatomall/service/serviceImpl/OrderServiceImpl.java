package com.example.tomatomall.service.serviceImpl;/*
 * @date 04/02 19:52
 */

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.easysdk.factory.Factory;
import com.example.tomatomall.config.AliPayConfig;
import com.example.tomatomall.dao.*;
import com.example.tomatomall.dto.PaymentNotifyDTO;
import com.example.tomatomall.po.*;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.service.StockpileService;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.tomatomall.config.RabbitMQConfig;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Resource
    private OrderRepository orderRepository;

    @Resource
    private CartItemRepository cartRepository;

    @Resource
    private ProductRepository productRepository;

    @Autowired
    private CartOrderRelationRepository cartOrderRelationRepository;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockpileService stockpileService;

    @Autowired
    private AccountRepository accountRepository;

    @Resource
    private AliPayConfig aliPayConfig;

    private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do"; // 支付宝沙箱环境地址
    private static final String FORMAT = "JSON";
    private static final String CHARSET = "utf-8";
    private static final String SIGN_TYPE = "RSA2";
    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ConcurrentHashMap<Integer, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    //使用可重入锁 added by cz on 4.9 at 19:47

    @Override
    @Transactional
    /*
        * 创建订单
        * 陈展 0409 10：54 重构了代码 分拆成多个方法
     */
    public Order createOrder(Integer userId, List<Integer> cartItemIds, Object shippingAddress, String paymentMethod) {
        // 1. 批量获取并验证数据
        List<CartItem> validCartItems = getAndValidateCartItems(userId, cartItemIds);

        Map<Integer, Product> productMap = getProductMap(validCartItems);

        // 2. 处理库存并计算总金额
        BigDecimal totalAmount = processStockAndCalculateTotal(validCartItems, productMap);

        // 3. 创建订单并保存关联
        Order order = createOrderEntity(userId, paymentMethod, totalAmount);
        saveCartOrderRelations(validCartItems, order);
        sendDelayOrderMessage(order);
        return order;
    }

    // 在OrderServiceImpl中修改sendDelayOrderMessage方法
    private void sendDelayOrderMessage(Order order) {
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");  // 显式设置内容类型
        props.setDelay(7200 * 1000);               // 使用标准延时设置

        Message message = rabbitTemplate.getMessageConverter().toMessage(
                new HashMap<String, Object>() {{
                    put("orderId", order.getOrderId());
                    put("createTime", order.getCreateTime().getTime());
                    put("expireDuration", 7200);
                }},
                props
        );

        rabbitTemplate.send(
                "order.delay.exchange",
                "order.delay",
                message
        );
    }


    /**
     * 获取并验证购物车项 (合并数据查询和权限验证)
     */
    private List<CartItem> getAndValidateCartItems(Integer userId, List<Integer> cartItemIds) {
        // 批量查询购物车项并过滤所属用户
        List<CartItem> cartItems = cartItemRepository.findAllByCartItemIdIn(cartItemIds);
        for (Integer cartItemId : cartItemIds) {
            log.info("输入的cartItemId: {}", cartItemId);
        }
        for (CartItem cartItem : cartItems) {
            log.info("找到的cartItemId: {} ProductId: {}", cartItem.getCartItemId(),cartItem.getProductId());
        }

        return cartItems;
    }

    /**
     * 处理库存并计算总金额 (合并库存操作和金额计算)
     */

    private BigDecimal processStockAndCalculateTotal(List<CartItem> cartItems, Map<Integer, Product> productMap) {
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            Integer productId = cartItem.getProductId();

            // 获取或创建该商品的锁
            ReentrantLock lock = productLocks.computeIfAbsent(productId, k -> new ReentrantLock());

            try {
                lock.lock();

                Stockpile stockpile = productService.getStock(productId);

                if (stockpile.getAmount() < cartItem.getQuantity()) {
                    throw new RuntimeException("商品库存不足: " + product.getTitle());
                }

                stockpile.setAmount(stockpile.getAmount() - cartItem.getQuantity());
                stockpile.setLockedAmount(stockpile.getLockedAmount() + cartItem.getQuantity());

                stockpileService.updateStockpile(stockpile);
            } finally {
                lock.unlock();
            }

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            log.info("当前商品: {} 数量: {} 单价: {} 总金额: {}",
                    product.getTitle(), cartItem.getQuantity(), product.getPrice(), total);
        }

        return total;
    }
    /**
     * 获取商品映射表 (合并商品查询和存在性验证)
     */
    private Map<Integer, Product> getProductMap(List<CartItem> cartItems) {
        Set<Integer> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap = new HashMap<>();
        List<Product> products = productRepository.findAllById(productIds);

        // 遍历产品列表，将每个产品映射到其ID
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }

        // 验证所有商品存在
        cartItems.forEach(cartItem -> {
            if (!productMap.containsKey(cartItem.getProductId())) {
                throw new RuntimeException("商品不存在: " + cartItem.getProductId());
            }
        });

        return productMap;
    }

    /**
     * 创建订单实体 (封装订单创建逻辑)
     */
    private Order createOrderEntity(Integer userId, String paymentMethod, BigDecimal totalAmount) {
        Order order = new Order();
        order.setUserId(userId);
        //为了满足67在创建订单时需要返回用户名的需求 0409 19:54
        String username = accountRepository.findByUserId(userId).getUsername();
        order.setUsername(username);

        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("PENDING");
        order.setLockExpireTime(new Timestamp(System.currentTimeMillis() + 120 * 60 * 1000));
        //为了调试方便 我先设置120分钟的过期时间 modified by cz on 4.9 at 20:32
        log.info("创建订单: 用户ID {}, 支付方式 {}, 总金额 {}", userId, paymentMethod, totalAmount);
        return orderRepository.save(order);
    }

    /**
     * 保存购物车-订单关联 (批量操作优化)
     * 使用静态工厂方式创建 CartOrderRelation 实例
     */
    private void saveCartOrderRelations(List<CartItem> cartItems, Order order) {
        List<CartOrderRelation> relations = new ArrayList<>(cartItems.size());

        for (CartItem cartItem : cartItems) {
            relations.add(CartOrderRelation.of(cartItem.getCartItemId(), order.getOrderId()));
        }

        cartOrderRelationRepository.saveAll(relations);
    }

    @Override
    public Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单未找到"));
    }

    @Override
    public Order getOrderByTradeNo(String tradeNo) {
        return orderRepository.findByTradeNo(tradeNo)
                .orElseThrow(() -> new RuntimeException("订单未找到"));
    }

    @Override
    public List<Order> getOrdersByUserId(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Integer orderId, String status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }



    // 支付回调处理
    @Override
    @Transactional
    public boolean processPaymentCallback(PaymentNotifyDTO paymentNotifyDTO) throws Exception {
        // 参数解析
        String orderId = paymentNotifyDTO.getOutTradeNo();
        String alipayTradeNo = paymentNotifyDTO.getTradeNo();
        BigDecimal actualAmount = paymentNotifyDTO.getTotalAmount();

        // 获取订单
        Order order = getOrderById(Integer.parseInt(orderId));

        // 1. 金额校验
        if (order.getTotalAmount().compareTo(actualAmount) != 0) {
            log.warn("订单金额不一致，订单ID：{} 系统金额：{} 实际金额：{}",
                    orderId, order.getTotalAmount(), actualAmount);
            throw new RuntimeException("支付金额校验失败");
        }

        // 2. 状态检查（二次幂等校验）
        if ("SUCCESS".equals(order.getStatus())) {
            log.info("订单已处理，直接返回成功，订单ID：{}", orderId);
            return true;
        }

        // 3. 库存处理
        List<CartOrderRelation> relations = cartOrderRelationRepository.findByOrderId(order.getOrderId());
        relations.forEach(relation -> {
            CartItem cartItem = cartRepository.findById(relation.getCartItemId())
                    .orElseThrow(() -> new RuntimeException("购物车项不存在"));

            Stockpile stockpile = stockpileRepository.findByProductId(cartItem.getProductId()).get();
            synchronized (stockpile) {
                // 释放锁定库存（实际库存已在创建订单时扣减）
                stockpile.setLockedAmount(stockpile.getLockedAmount() - cartItem.getQuantity());
                stockpileRepository.save(stockpile);
            }
        });
        log.info("已经释放锁定库存，订单ID：{}", orderId);
        // 4. 清理购物车数据
        List<Integer> cartItemIds = relations.stream()
                .map(CartOrderRelation::getCartItemId)
                .collect(Collectors.toList());
        cartRepository.deleteAllById(cartItemIds);
        cartOrderRelationRepository.deleteAll(relations);
        log.info("已经清理购物车数据，订单ID：{}", orderId);
        // 5. 更新订单状态
        order.setStatus("SUCCESS");
        order.setTradeNo(alipayTradeNo);
        order.setPaymentTime(new Timestamp(System.currentTimeMillis()));
        orderRepository.save(order);
        log.info("订单状态更新成功，订单ID：{} orderStatus：{} tradeNo: {} PaymentTime", orderId,"SUCCESS",order.getTradeNo(), order.getPaymentTime());
        return true;
    }



    @Transactional
    @Override
    public void handleExpiredOrder(Integer orderId) {
        Order order = getOrderById(orderId);
        if ("PENDING".equals(order.getStatus()) &&
                order.getLockExpireTime().before(new Timestamp(System.currentTimeMillis()))) {

            // 恢复库存
            List<CartOrderRelation> relations = cartOrderRelationRepository.findByOrderId(orderId);
            relations.forEach(relation -> {
                CartItem cartItem = cartRepository.findById(relation.getCartItemId()).orElse(null);
                if (cartItem != null) {
                    Stockpile stockpile = productService.getStock(cartItem.getProductId());
                    synchronized (this) {
                        stockpile.setAmount(stockpile.getAmount() + cartItem.getQuantity());
                        stockpile.setLockedAmount(stockpile.getLockedAmount() - cartItem.getQuantity());
                        productService.updateStock(stockpile.getProductId(),stockpile.getAmount());
                    }
                }
            });

            // 更新订单状态
            order.setStatus("TIMEOUT");
            orderRepository.save(order);
        }

    }


    @Override
    public List<Order> findExpiredOrders() {
        // 获取当前时间戳
        Timestamp now = new Timestamp(System.currentTimeMillis());
        // 查询状态为PENDING且锁定时间早于当前时间的订单
        return orderRepository.findByStatusAndLockExpireTimeBefore("PENDING", now);
    }



}