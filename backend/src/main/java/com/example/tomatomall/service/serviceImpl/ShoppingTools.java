package com.example.tomatomall.service.serviceImpl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.vo.CartItemVO;
import com.example.tomatomall.vo.CartVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.tomatomall.config.AliPayConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShoppingTools {

    @Autowired
    private CartServiceImpl cartService;

    @Autowired
    private ProductServiceImpl productService;

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private AliPayConfig aliPayConfig;

    @Value("${alipay.returnUrl}")
    private String returnUrl;


    @Tool("当用户需要添加商品到购物车时使用。如果未明确指定商品ID，会自动关联用户最近被推荐的商品。默认添加数量为1件。")
    public String addToCart(
            @ToolMemoryId Integer userId,
            @P(value = "要添加的商品唯一标识符。如果未提供，将自动使用最近推荐给用户的商品ID", required = false) Integer productId,
            @P(value = "需要添加的商品件数。必须是1-100之间的正整数，默认自动添加1件", required = false) Integer quantity
    ) {
        System.out.println("[Tool Start] addToCart - UserID: " + userId);

        try {
            if (productId == null) {
                System.out.println("[QueryMemory] Using recommended product ID: " + productId);
            }

            int qty = (quantity != null && quantity > 0) ? quantity : 1;
            qty = Math.min(qty, 100);

            Product product = productService.getProduct(productId);
            System.out.println("[DB Query] Retrieved product: ID=" + productId + ", Title=" + (product != null ? product.getTitle() : "null"));

            cartService.addCartItem(userId, productId, qty);
            System.out.println("[Tool End] addToCart - Successfully added product " + productId + " for user " + userId);
            return String.format("已成功将《%s》%d件加入购物车（商品ID：%d）",
                    product.getTitle(), qty, productId);
        } catch (Exception e) {
            System.out.println("[Tool Error] addToCart - Failed for user " + userId + ": " + e.getMessage());
            return "操作失败：" + e.getMessage();
        }
    }

    @Tool("获取用户购物车的完整内容，包括商品名称、单价、数量、评分和规格信息。当用户询问'我的购物车里有什么'或'查看购物车'时使用。")
    public String viewCartContent(
            @ToolMemoryId Integer userId
    ) {
        System.out.println("[Tool Start] viewCartContent - UserID: " + userId);
        try {
            CartVO cart = cartService.getCartItems(userId);
            System.out.println("[DB Query] Retrieved cart items count: " + (cart != null ? cart.getItems().size() : 0));

            if (cart == null || cart.getItems().isEmpty()) {
                System.out.println("[Tool End] viewCartContent - Empty cart for user " + userId);
                return "您的购物车目前是空的";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("您的购物车中有").append(cart.getItems().size()).append("件商品：\n");

            double totalPrice = 0.0;  // Initialize total price
            for (CartItemVO item : cart.getItems()) {
                Product product = productService.getProduct(item.getProductId());
                System.out.println("[DB Query] Cart item details - ProductID: " + item.getProductId() +
                        ", Title: " + (product != null ? product.getTitle() : "null") +
                        ", Quantity: " + item.getQuantity());

                double itemTotal = product.getPrice().doubleValue() * item.getQuantity();
                totalPrice += itemTotal;  // Update total price
                sb.append("- 《").append(product.getTitle()).append("》\n")
                        .append("  ▶ 单价：").append(product.getPrice()).append("元")
                        .append("\n  ▶ 数量：").append(item.getQuantity())
                        .append("\n  ▶ 小计：").append(String.format("%.2f", itemTotal)).append("元")
                        .append("\n  ▶ 评分：").append(String.format("%.1f/5.0", product.getRate()))
                        .append("\n  ▶ 规格：");

                // 添加商品规格（最多显示10条）
                if (product.getSpecifications() != null && !product.getSpecifications().isEmpty()) {
                    product.getSpecifications().stream()
                            .limit(10)
                            .forEach(spec -> {
                                sb.append(spec.getItem()).append(":").append(spec.getValue()).append(" ");
                            });
                } else {
                    sb.append("无规格");
                }
                sb.append("\n");
            };

            // Add total price to the output
            sb.append("\n总计：").append(String.format("%.2f", totalPrice)).append("元");

            // Debug output for total price
            System.out.println("[Cart Total] Calculated total price: " + String.format("%.2f", totalPrice));
            System.out.println("[Tool End] viewCartContent - Successfully viewed cart for user " + userId);

            return sb.toString();
        } catch (Exception e) {
            System.out.println("[Tool Error] viewCartContent - Failed for user " + userId + ": " + e.getMessage());
            return "查看购物车失败：" + e.getMessage();
        }
    }

    @Tool("使用购物车中的商品生成订单并引导用户完成支付。自动使用默认收货地址和支付宝支付方式" +
            "请你提取html代码中的网址返回。如工具的输出是<html><head><meta charset=\"utf-8\"></head><body><form name=\"punchout_form\" method=\"post\" action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=eG%2B6tWZpsrU1Y1HrFWPS7WIbt28z3NKH1MBxKIuvMIW8P5AoZXRAWfVek0PuBQOBOngh8WdmrOK9ZbJh87grEhnRDcaDQaz5FocDvAfzsCLifE72LZWR7OebkgoJ%2FW3JWE3Yw%2BAlK4IHCj%2Fa%2FMGE3vifnYrPnhaMEHjUPCcsslMeivPBYSX0czDSakfMakY2PEc5uPXw6evRLBxK0H9nf5aStDAiYEKvz1ucJZYWkZP8Dm%2FacGlYO12EYBDjZmcohM%2BFfs9qjD0czxlX6Undr1f%2BLQJ%2FwdMhjogGl32bpIXcin96bCAIj53JIZN6BQ7pBuCW0hII6Gc6R5EOUzgwwA%3D%3D&return_url=http%3A%2F%2Fdncu2p.natappfree.cc%2Fapi%2Forders%2Freturn&notify_url=http%3A%2F%2Fdncu2p.natappfree.cc%2Fapi%2Forders%2Fnotify&version=1.0&app_id=2021000147671450&sign_type=RSA2&timestamp=2025-05-15+20%3A17%3A38&alipay_sdk=alipay-sdk-java-dynamicVersionNo&format=JSON\">\n" +
            "<input type=\"hidden\" name=\"biz_content\" value=\"{&quot;out_trade_no&quot;:14,&quot;total_amount&quot;:998.80,&quot;subject&quot;:&quot;Tomato Mall Order #14&quot;,&quot;product_code&quot;:&quot;FAST_INSTANT_TRADE_PAY&quot;}\">\n" +
            "<input type=\"submit\" value=\"立即支付\" style=\"display:none\" >\n" +
            "</form>\n" +
            "<script>document.forms[0].submit();</script></body></html>请你提取https://openapi-sandbox.dl.alipaydev.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=eG%2B6tWZpsrU1Y1HrFWPS7WIbt28z3NKH1MBxKIuvMIW8P5AoZXRAWfVek0PuBQOBOngh8WdmrOK9ZbJh87grEhnRDcaDQaz5FocDvAfzsCLifE72LZWR7OebkgoJ%2FW3JWE3Yw%2BAlK4IHCj%2Fa%2FMGE3vifnYrPnhaMEHjUPCcsslMeivPBYSX0czDSakfMakY2PEc5uPXw6evRLBxK0H9nf5aStDAiYEKvz1ucJZYWkZP8Dm%2FacGlYO12EYBDjZmcohM%2BFfs9qjD0czxlX6Undr1f%2BLQJ%2FwdMhjogGl32bpIXcin96bCAIj53JIZN6BQ7pBuCW0hII6Gc6R5EOUzgwwA%3D%3D&return_url=http%3A%2F%2Fdncu2p.natappfree.cc%2Fapi%2Forders%2Freturn&notify_url=http%3A%2F%2Fdncu2p.natappfree.cc%2Fapi%2Forders%2Fnotify&version=1.0&app_id=2021000147671450&sign_type=RSA2&timestamp=2025-05-15+20%3A17%3A38&alipay_sdk=alipay-sdk-java-dynamicVersionNo&format=JSON\"返回")
    public String checkoutAndPay(
            @ToolMemoryId Integer userId
    ) {
        System.out.println("[Tool Start] checkoutAndPay - UserID: " + userId);
        try {
            // 创建订单
            CartVO cart = cartService.getCartItems(userId);
            if (cart == null || cart.getItems().isEmpty()) {
                return "您的购物车是空的，无法创建订单";
            }

            List<Integer> cartItemIds = cart.getItems().stream()
                    .map(CartItemVO::getCartItemId)
                    .collect(Collectors.toList());

            Order order = orderService.createOrder(userId, cartItemIds, new Object(), "Alipay");

            // 直接处理支付
            if (order.getLockExpireTime().before(new java.util.Date())) {
                throw new RuntimeException("订单已过期，请重新下单");
            }

            // 构建支付宝客户端
            AlipayClient alipayClient = new DefaultAlipayClient(
                    "https://openapi-sandbox.dl.alipaydev.com/gateway.do",
                    aliPayConfig.getAppId(),
                    aliPayConfig.getAppPrivateKey(),
                    "JSON",
                    "utf-8",
                    aliPayConfig.getAlipayPublicKey(),
                    "RSA2");

            // 创建支付请求
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(aliPayConfig.getNotifyUrl());
            request.setReturnUrl(returnUrl);

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", order.getOrderId());
            bizContent.put("total_amount", order.getTotalAmount());
            bizContent.put("subject", "Tomato Mall Order #" + order.getOrderId());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());

            // 获取支付表单
            String form = alipayClient.pageExecute(request).getBody();

            // 构造完整HTML响应
            String htmlResponse = "<html><head><meta charset=\"utf-8\"></head><body>" +
                    form +
                    "</body></html>";

            System.out.println("[Tool End] checkoutAndPay - 生成支付表单 html: " + htmlResponse);
            return htmlResponse;

        } catch (AlipayApiException e) {
            System.err.println("支付宝接口调用异常: " + e.getMessage());
            return "支付系统异常，请稍后重试";
        } catch (Exception e) {
            System.err.println("订单处理异常: " + e.getMessage());
            return "订单创建失败: " + e.getMessage();
        }
    }

    @Tool("从购物车移除指定商品。当用户说'删除商品'或'移出购物车'时使用。需要明确商品ID。")
    public String removeFromCart(
            @ToolMemoryId Integer userId,
            @P(value = "要移除的商品ID", required = true) Integer productId
    ) {
        System.out.println("[Tool Start] removeFromCart - UserID: " + userId + ", ProductID: " + productId);

        try {
            // 获取购物车并查找对应商品
            CartVO cart = cartService.getCartItems(userId);
            CartItemVO targetItem = cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst()
                    .orElse(null);


            if (targetItem == null) {
                System.out.println("[Validation] Product not found in cart");
                return "购物车中未找到该商品";
            }

            // 执行删除
            cartService.removeCartItem(targetItem.getCartItemId());
            Product product = productService.getProduct(productId);

            System.out.println("[Tool End] Successfully removed product " + productId);
            return String.format("已从购物车移除《%s》", product.getTitle());
        } catch (Exception e) {
            System.out.println("[Tool Error] removeFromCart - " + e.getMessage());
            return "移除失败：" + e.getMessage();
        }
    }
}