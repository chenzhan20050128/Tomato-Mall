package com.example.tomatomall.dao;

import com.example.tomatomall.po.CartOrderRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartOrderRelationRepository extends JpaRepository<CartOrderRelation, Integer> {
    List<CartOrderRelation> findByOrderId(Integer orderId);
}