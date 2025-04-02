package com.example.tomatomall.service;

import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.Response;

public interface CartService {
    CartItem addCartItem(Integer userId, Integer productId, Integer quantity);

    String removeCartItem(Integer cartItemId);

    String updateCartItemQuantity(Integer cartItemId, Integer quantity);

    CartVO getCartItems(Integer userId);
}