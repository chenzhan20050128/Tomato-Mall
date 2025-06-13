package com.example.tomatomall.dao;

import com.example.tomatomall.po.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    /**
     * 查询指定分类的高评分商品
     * @param limit 限制数量
     * @param category 商品分类（可选）
     * @return 商品ID、平均评分和评分数量
     */
    @Query(value = "SELECT r.product_id, AVG(r.score) as avg_score, COUNT(*) as rating_count " +
            "FROM ratings r " +
            "JOIN products p ON r.product_id = p.id " +
            "JOIN specifications s ON s.product_id = p.id " +
            "WHERE (:category IS NULL OR (s.item = '分类' AND s.value = :category)) " +
            "GROUP BY r.product_id " +
            "ORDER BY avg_score DESC, rating_count DESC " +
            "LIMIT :limit", nativeQuery = true)

    List<Object[]> findTopRatedProducts(
            @Param("limit") int limit,
            @Param("category") String category);
    @Query(value = "SELECT r.product_id, AVG(r.score) as avg_score, COUNT(*) as rating_count " +
            "FROM ratings r " +
            "JOIN products p ON r.product_id = p.id " +
            "JOIN specifications s ON s.product_id = p.id " +
            "GROUP BY r.product_id " +
            "ORDER BY avg_score DESC, rating_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopRatedProducts(
            @Param("limit") int limit);

    /**
     * Find ratings by product ID
     */
    List<Rating> findByProductId(Integer productId);

    /**
     * Find ratings by user ID
     */
    List<Rating> findByUserId(Integer userId);

    /**
     * Count ratings for a product
     */
    long countByProductId(Integer productId);

    /**
     * Find average score for a product
     */
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.productId = :productId AND r.isValidScore() = true")
    Double findAverageScoreByProductId(Integer productId);
}