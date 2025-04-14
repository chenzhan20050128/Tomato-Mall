package com.example.tomatomall.dao;

import com.example.tomatomall.po.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findById(Integer productId);

    @Query("SELECT p FROM Product p WHERE p.id IN :productids")
    List<Product> findAllById(@Param("productids") List<Integer> productids);

    @Query(
            value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.specifications",
            countQuery = "SELECT count(DISTINCT p) FROM Product p" // 修改 count 查询
    )
    Page<Product> findAllWithSpecifications(Pageable pageable);


    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.specifications WHERE p.id = :id")
    Product findByIdWithSpecifications(@Param("id") Integer id);
}