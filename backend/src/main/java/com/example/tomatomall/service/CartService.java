package com.example.tomatomall.service;

import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.Response;

public interface CartService {
    Response<CartItem> addCartItem(Integer userId, Integer productId, Integer quantity);

    Response<String> removeCartItem(Integer cartItemId);

    Response<String> updateCartItemQuantity(Integer cartItemId, Integer quantity);

    Response<CartVO> getCartItems(Integer userId);
}