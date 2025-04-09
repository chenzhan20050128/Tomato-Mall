package com.example.tomatomall.dao;

import com.example.tomatomall.po.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserId(Integer userId); // 根据用户ID查找订单

    Optional<Order> findByTradeNo(String tradeNo);

    List<Order> findByStatusAndLockExpireTimeBefore(String status, Timestamp currentTime);
}