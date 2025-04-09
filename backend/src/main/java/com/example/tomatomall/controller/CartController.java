package com.example.tomatomall.controller;

import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.CheckoutRequest;
import com.example.tomatomall.vo.Response;
import com.example.tomatomall.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@RestController
@RequestMapping("/api/cart")
@Slf4j
public class CartController {

    @Autowired
    private OrderService orderService;

    @Resource
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Response<CartItem> addCartItem(@RequestBody CartItem item, @RequestAttribute("userId") Integer userId) {
        try {
            CartItem cartItem = cartService.addCartItem(userId, item.getProductId(), item.getQuantity());
            return Response.buildSuccess(cartItem);
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        }
    }

    @DeleteMapping("/{cart_item_id}")
    public Response<String> removeCartItem(@PathVariable("cart_item_id") Integer cartItemId) {
        try {
            cartService.removeCartItem(cartItemId);
            return Response.buildSuccess("删除成功");
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        }
    }

    @PatchMapping("/{cart_item_id}/quantity")
    public Response<String> updateCartItemQuantity(@PathVariable("cart_item_id") Integer cartItemId,
                                                   @RequestBody CartItem item) {
        try {
            cartService.updateCartItemQuantity(cartItemId, item.getQuantity());
            return Response.buildSuccess("更新成功");
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        }
    }

    @GetMapping
    public Response<CartVO> getCartItems(@RequestAttribute("userId") Integer userId) {
        try {
            CartVO cartVO = cartService.getCartItems(userId);
            return Response.buildSuccess(cartVO);
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        }
    }

    @PostMapping("/checkout")
    public Response<Order> checkout(@RequestBody CheckoutRequest request,@RequestAttribute("userId") Integer userId) {
        try {
            if (userId == null) {
                return Response.buildFailure("token无法解析userId", "401");
            }
            log.info("checkout:userId: {}", userId);

            if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
                return Response.buildFailure("请选择要结算的商品", "400");
            }
            if (request.getShippingAddress() == null) {
                return Response.buildFailure("收货地址不能为空", "400");
            }
            if (request.getPaymentMethod() == null) {
                return Response.buildFailure("请选择支付方式", "400");
            }

            // 调用订单服务创建订单
            Order order = orderService.createOrder(
                    userId, // 使用从token中解析出的userId
                    request.getCartItemIds(),
                    request.getShippingAddress(),
                    request.getPaymentMethod()
            );

            // 构建响应数据
            return Response.buildSuccess(order);
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("系统错误，创建订单失败", "500");
        }
    }
}