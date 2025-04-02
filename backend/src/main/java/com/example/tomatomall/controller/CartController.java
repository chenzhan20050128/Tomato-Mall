package com.example.tomatomall.controller;

import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.vo.CartVO;
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
        CartItem cartItem = cartService.addCartItem(userId, item.getProductId(), item.getQuantity());
        if (cartItem == null) {
            return Response.buildFailure("添加购物车失败", "400");
        }
        return Response.buildSuccess(cartItem);
    }

    @DeleteMapping("/{cart_item_id}")
    public Response<String> removeCartItem(@PathVariable("cart_item_id") Integer cartItemId) {
        String result = cartService.removeCartItem(cartItemId);
        if (result == "删除成功"){
            return Response.buildSuccess(result);
        } else {
            return Response.buildFailure(result, "400");
        }
    }

    @PatchMapping("/{cart_item_id}/quantity")
    public Response<String> updateCartItemQuantity(@PathVariable("cart_item_id") Integer cartItemId,
                                                   @RequestBody CartItem item) {
        String result = cartService.updateCartItemQuantity(cartItemId, item.getQuantity());

        if (result == "更新成功"){
            return Response.buildSuccess(result);
        } else {
            return Response.buildFailure(result, "400");
        }
    }

    @GetMapping
    public Response<CartVO> getCartItems(@RequestAttribute("userId") Integer userId) {
        return Response.buildSuccess(cartService.getCartItems(userId));
    }
}