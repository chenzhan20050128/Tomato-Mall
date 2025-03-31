package com.example.tomatomall.dao;

import com.example.tomatomall.po.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.specifications")
    List<Product> findAllWithSpecifications();
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.specifications WHERE p.id = :id")
    Product findByIdWithSpecifications(@Param("id") Integer id);
}