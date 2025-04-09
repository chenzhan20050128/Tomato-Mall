package com.example.tomatomall.dao;

import com.example.tomatomall.po.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findById(Integer productId);

    @Query("SELECT p FROM Product p WHERE p.id IN :productids")
    List<Product> findAllById(@Param("productids") List<Integer> productids);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.specifications")
    List<Product> findAllWithSpecifications();

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.specifications WHERE p.id = :id")
    Product findByIdWithSpecifications(@Param("id") Integer id);
}