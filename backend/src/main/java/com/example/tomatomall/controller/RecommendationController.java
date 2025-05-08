package com.example.tomatomall.controller;/*
 * @date 04/21 15:20
 */
import com.example.tomatomall.dto.ProductRankingDTO;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.RecommendationService;

import com.example.tomatomall.vo.Response;
import kotlin.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/*
推荐系统的完整结构如下：

Controller层
RecommendationController：处理API请求，返回各类推荐结果
Service层
RecommendationService：接口定义推荐相关方法
RecommendationServiceImpl：实现推荐逻辑，包括：
畅销榜 (getBestSellers)
评分榜 (getTopRated)
随机推荐 (getRandomRecommendations)
Repository层
ProductRepository：提供商品查询方法，如:

findBestSellingProducts：查询畅销商品
findRandomAvailableProducts：查询随机可用商品
findByCategoryIn：根据分类查询
findCategoryByProductId：获取商品分类
RatingRepository：提供评分查询方法，如:

findTopRatedProducts：查询高评分商品
DTO层
ProductRankingDTO：商品排行信息传输对象
RecommendationDTO：推荐结果传输对象，包含基于历史和随机两部分
实体层
Product：商品实体
Rating：评分实体
OrderItem：订单项实体，记录销量数据
数据库表
product：商品表
rating：评分表
order_item：订单项表，记录销售数据
系统优化建议
缺少用户历史浏览/购买记录的处理：

需要添加UserHistoryRepository来获取用户的浏览和购买记录
实现getUserPreferredCategories和getHistoryBasedRecommendations方法
缓存策略优化：

根据文档要求，榜单数据应每日凌晨更新
可以使用Spring的定时任务加载并更新缓存
数据可信度处理：

评分榜需要过滤掉评分次数少于10次的商品
销量计算需要排除异常订单
完善随机推荐：

当前随机推荐逻辑已基本实现，但需加强基于用户历史的个性化推荐
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取畅销书榜单
     * @param category 分类（可选）
     * @param days 时间范围（天）
     * @param limit 返回数量
     * @return 畅销书榜单
     */
    @GetMapping("/bestsellers")
    public Response<List<ProductRankingDTO>> getBestSellers(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {

        return Response.buildSuccess(recommendationService.getBestSellers(category, days, limit));
    }

    /**
     * 获取评分榜
     * @param category 分类（可选）
     * @param limit 返回数量
     * @return 评分榜
     */
    @GetMapping("/top-rated")
    public Response<List<ProductRankingDTO>> getTopRated(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit) {

        return Response.buildSuccess(recommendationService.getTopRated(category, limit));
    }

    /**
     * 获取随机推荐
     * @param userId 用户ID（可选，用于基于历史的推荐）
     * @param count 推荐数量
     * @return 随机推荐
     */
    @GetMapping("/random")
    public Response<List<ProductRankingDTO>> getRandomRecommendations(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "6") int count) {

        // 这里需要调用用户历史服务获取偏好分类
        List<String> preferredCategories = userId != null ?
                recommendationService.getUserPreferredCategories(userId) : null;
        // 随机推荐
        List<ProductRankingDTO> randomRecommendations =
                recommendationService.getRandomRecommendations(count, preferredCategories);

        return Response.buildSuccess(randomRecommendations);
    }
}