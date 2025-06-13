package com.example.tomatomall.dao;

import com.example.tomatomall.po.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;

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

    @EntityGraph(attributePaths = "specifications")
    @Query(
            value = "SELECT p FROM Product p LEFT JOIN FETCH p.specifications",
            countQuery = "SELECT COUNT(p) FROM Product p" // 简单COUNT查询
    )
    Page<Product> findAllForRag(Pageable pageable);

 // 修改后的方法 - 使用JOIN获取category
    @Query("SELECT spec.value FROM Product p JOIN p.specifications spec " +
           "WHERE p.id = :productId AND LOWER(spec.item) = '分类'")
    String findCategoryByProductId(@Param("productId") Integer productId);
    
    // 1. 修改畅销书查询
    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) AS total_sales " +
        "FROM order_items oi " +
        "JOIN orders o ON oi.order_id = o.order_id " +
        "JOIN products p ON oi.product_id = p.id " +
        "LEFT JOIN specifications s ON p.id = s.product_id AND s.item = '分类' " +
        "WHERE o.create_time >= :startDate " +
        "AND (:category IS NULL OR s.value = :category) " +
        "GROUP BY oi.product_id " +
        "ORDER BY total_sales DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<Object[]> findBestSellingProducts(
            @Param("startDate") Date startDate, 
            @Param("limit") int limit, 
            @Param("category") String category);
    
    // 修改类别列表查询
    @Query("SELECT DISTINCT p FROM Product p JOIN p.specifications spec " +
           "WHERE spec.item = '分类' AND spec.value IN :categories")
    List<Product> findByCategoryIn(@Param("categories") List<String> categories, Pageable pageable);
    
    // 随机查询可以保持不变
    @Query(value = "SELECT * FROM products WHERE id IN " +
           "(SELECT id FROM products ORDER BY RAND() LIMIT :limit)", 
           nativeQuery = true)
    List<Product> findRandomAvailableProducts(@Param("limit") int limit);
    
    // 修改用户历史类别查询
    @Query(value = "SELECT DISTINCT s.value " +
           "FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.order_id " +
           "JOIN products p ON oi.product_id = p.id " +
           "JOIN specifications s ON p.id = s.product_id AND LOWER(s.item) = '分类' " +
           "WHERE o.user_id = :userId ",
           nativeQuery = true)
    List<String> findCategoriesByUserOrderHistory(@Param("userId") Integer userId);

}