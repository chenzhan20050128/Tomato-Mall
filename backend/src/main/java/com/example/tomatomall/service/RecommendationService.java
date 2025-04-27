package com.example.tomatomall.service;

import com.example.tomatomall.dto.ProductRankingDTO;

import java.util.List;

public interface RecommendationService {
    /**
     * 获取畅销商品排行榜
     * @param category 商品分类（可选）
     * @param days 统计天数
     * @param limit 返回数量
     * @return 畅销商品列表
     */
    List<ProductRankingDTO> getBestSellers(String category, int days, int limit);

    /**
     * 获取高评分商品排行榜
     * @param category 商品分类（可选）
     * @param limit 返回数量
     * @return 高评分商品列表
     */
    List<ProductRankingDTO> getTopRated(String category, int limit);

    /**
     * 获取随机推荐商品
     * @param count 返回数量
     * @param preferredCategories 偏好的商品分类列表（可选）
     * @return 随机推荐商品列表
     */
    List<ProductRankingDTO> getRandomRecommendations(int count, List<String> preferredCategories);

    List<String> getUserPreferredCategories(Integer userId);
}