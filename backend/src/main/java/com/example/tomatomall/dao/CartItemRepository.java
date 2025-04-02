package com.example.tomatomall.dao;

import com.example.tomatomall.po.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByUserId(Integer userId);
    List<CartItem> findByUserIdAndProductId(Integer userId, Integer productId);
}