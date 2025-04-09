package com.example.tomatomall.service;

import com.example.tomatomall.po.CartItem;
import com.example.tomatomall.vo.CartVO;

public interface CartService {
    CartItem addCartItem(Integer userId, Integer productId, Integer quantity);

    void removeCartItem(Integer cartItemId);

    void updateCartItemQuantity(Integer cartItemId, Integer quantity);

    CartVO getCartItems(Integer userId);
}