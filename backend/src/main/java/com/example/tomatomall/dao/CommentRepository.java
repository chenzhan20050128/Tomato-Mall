package com.example.tomatomall.dao;

import com.example.tomatomall.po.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // 保留原方法
    Page<Comment> findByProductIdAndStatusOrderByCreateTimeDesc(Integer productId, Integer status, Pageable pageable);

    // 添加原生SQL查询方法
    @Query(value = "SELECT * FROM comments WHERE product_id = :productId AND status = :status ORDER BY create_time DESC",
            nativeQuery = true)
    Page<Comment> findCommentsByProductIdAndStatusNative(
            @Param("productId") Integer productId,
            @Param("status") Integer status,
            Pageable pageable);

    // 添加一个简单的计数方法来验证
    @Query(value = "SELECT COUNT(*) FROM comments WHERE product_id = :productId AND status = :status",
            nativeQuery = true)
    int countCommentsByProductIdAndStatus(@Param("productId") Integer productId, @Param("status") Integer status);

    @Query("SELECT c.rating FROM Comment c WHERE c.productId = :productId AND c.status = :status")
    List<BigDecimal> findRatingsByProductIdAndStatus(@Param("productId") Integer productId, @Param("status") Integer status);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.productId = :productId")
    long countByProductId(@Param("productId") Integer productId);
}