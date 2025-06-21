// 修改后的RecommendationServiceImpl
package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dao.RatingRepository;
import com.example.tomatomall.dto.ProductRankingDTO;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final ProductRepository productRepo;
    private final RatingRepository ratingRepo;

    public RecommendationServiceImpl(ProductRepository productRepo,
            RatingRepository ratingRepo) {
        this.productRepo = productRepo;
        this.ratingRepo = ratingRepo;
    }

    // 修正畅销榜实现
    @Cacheable(value = "bestSellers", key = "#category + '-' + #days")
    public List<ProductRankingDTO> getBestSellers(String category, int days, int limit) {
        Date startDate = Date.from(LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 1. 获取销量数据
        List<Object[]> salesData = productRepo.findBestSellingProducts(
                startDate, limit, category);

        // 2. 提取商品ID
        List<Integer> productIds = salesData.stream()
                .map(data -> (Integer) data[0])
                .collect(Collectors.toList());

        // 3. 获取完整商品信息
        Map<Integer, Product> products = productRepo.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

       // 4. 构建DTO列表
    return salesData.stream()
            .map(data -> {
                Integer productId = ((Number) data[0]).intValue();
                
                // 修改这里：使用Number类型安全地获取销量
                int totalSales = (data[1] == null) ? 0 : ((Number) data[1]).intValue();
                
                Product product = products.get(productId);
                
                // 添加空值检查
                if (product == null) {
                    log.warn("未找到ID为{}的商品", productId);
                    return null;
                }

                return ProductRankingDTO.builder()
                        .productId(productId)
                        .title(product.getTitle())
                        .cover(product.getCover())
                        .price(product.getPrice().doubleValue())
                        .sales(totalSales)
                        .category(getProductCategory(productId))
                        .build();
            })
            .filter(Objects::nonNull) // 过滤掉null值
            .collect(Collectors.toList());
    }

    // 修正评分榜实现
//    @Cacheable(value = "topRated", key = "#category")
    public List<ProductRankingDTO> getTopRated(String category, int limit) {
        // 1. 获取评分数据
        List <Object[]> ratingData = null;
        if( category == null || category.isEmpty() || category.equals("null")) {
            ratingData = ratingRepo.findTopRatedProducts(limit);
        }else{
            ratingData = ratingRepo.findTopRatedProducts(limit, category);

        }

        // 2. 提取商品ID
        List<Integer> productIds = ratingData.stream()
                .map(data -> (Integer) data[0])
                .collect(Collectors.toList());

        // 3. 获取完整商品信息
        Map<Integer, Product> products = productRepo.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4. 构建DTO列表
        return ratingData.stream()
                .map(data -> {
                    Integer productId = (Integer) data[0];
                    Double avgScore = ((BigDecimal) data[1]).doubleValue(); // 修复类型转换
                    Number countNumber = (Number) data[2];
                    Long ratingCount = countNumber.longValue();
                    Product product = products.get(productId);

                    return ProductRankingDTO.builder()
                            .productId(productId)
                            .title(product.getTitle())
                            .cover(product.getCover())
                            .price(product.getPrice().doubleValue())
                            .averageScore(avgScore)
                            .ratingCount(ratingCount.intValue())
                            .category(getProductCategory(productId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 修正随机推荐实现
    public List<ProductRankingDTO> getRandomRecommendations(int count, List<String> preferredCategories) {
        List<Product> products;

        if (preferredCategories != null && !preferredCategories.isEmpty()) {
            // 带分类的随机推荐
            Pageable pageable = PageRequest.of(0, count * 2); // 获取更多结果用于随机选择
            products = productRepo.findByCategoryIn(preferredCategories, pageable);

            if (products.size() >= count) {
                Collections.shuffle(products);
                products = products.subList(0, count);
            } else {
                // 补充随机商品
                int remain = count - products.size();
                List<Product> randomProducts = productRepo.findRandomAvailableProducts(remain);
                products.addAll(randomProducts);
            }
        } else {
            // 完全随机推荐
            products = productRepo.findRandomAvailableProducts(count);
        }

        return products.stream()
                .map(product -> ProductRankingDTO.builder()
                        .productId(product.getId())
                        .title(product.getTitle())
                        .cover(product.getCover())
                        .price(product.getPrice().doubleValue())
                        .category(getProductCategory(product.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    // 辅助方法：获取商品分类
    private String getProductCategory(Integer productId) {
        // 实现分类查询逻辑（需要根据实际数据模型调整）
        return productRepo.findCategoryByProductId(productId);
    }

    // 新增 - 获取用户偏好分类
    @Override
    public List<String> getUserPreferredCategories(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 从用户历史订单中获取偏好分类
        List<String> orderBasedCategories = productRepo.findCategoriesByUserOrderHistory(userId);

        return orderBasedCategories;
    }

}
