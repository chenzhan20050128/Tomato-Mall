package com.example.tomatomall.controller;

import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Resource
    private CartService cartService;

    @PostMapping
    public Response<CartItem> addCartItem(@RequestBody CartItem item, @RequestAttribute("userId") Integer userId) {
        return cartService.addCartItem(userId, item.getProductId(), item.getQuantity());
    }

    @DeleteMapping("/{cart_item_id}")
    public Response<String> removeCartItem(@PathVariable("cart_item_id") Integer cartItemId) {
        return cartService.removeCartItem(cartItemId);
    }

    @PatchMapping("/{cart_item_id}/quantity")
    public Response<String> updateCartItemQuantity(@PathVariable("cart_item_id") Integer cartItemId,
                                                   @RequestBody CartItem item) {
        return cartService.updateCartItemQuantity(cartItemId, item.getQuantity());
    }

    @GetMapping
    public Response<?> getCartItems(@RequestAttribute("userId") Integer userId) {
        return cartService.getCartItems(userId);
    }
}