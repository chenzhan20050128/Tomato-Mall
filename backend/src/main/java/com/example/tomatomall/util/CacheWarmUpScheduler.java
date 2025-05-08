package com.example.tomatomall.util;

import com.example.tomatomall.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class CacheWarmUpScheduler {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每日凌晨3点自动预热缓存
     * 按照需求文档要求：榜单数据每日凌晨更新
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void warmUpCaches() {
        log.info("开始预热榜单缓存...");

        // 清除旧缓存
        redisTemplate.delete("bestSellers::*");
        redisTemplate.delete("topRated::*");

        // 预热畅销榜缓存 - 7天
        log.info("预热畅销榜(7天)缓存");
        warmUpBestSellers(7);

        // 预热畅销榜缓存 - 30天
        log.info("预热畅销榜(30天)缓存");
        warmUpBestSellers(30);

        // 预热畅销榜缓存 - 90天
        log.info("预热畅销榜(90天)缓存");
        warmUpBestSellers(90);

        // 预热评分榜缓存
        log.info("预热评分榜缓存");
        warmUpTopRated();

        log.info("榜单缓存预热完成");
    }

    /**
     * 预热畅销榜缓存，包括全部类别和主要类别
     */
    private void warmUpBestSellers(int days) {
        // 全部类别的畅销榜
        recommendationService.getBestSellers(null, days, 20);

        // 主要类别的畅销榜
        String[] mainCategories = {"计算机科学", "科幻"};
        for (String category : mainCategories) {
            recommendationService.getBestSellers(category, days, 10);
        }
    }

    /**
     * 预热评分榜缓存，包括全部类别和主要类别
     */
    private void warmUpTopRated() {
        // 全部类别的评分榜
        recommendationService.getTopRated(null, 20);

        // 主要类别的评分榜
        String[] mainCategories = {"计算机科学", "科幻"};
        for (String category : mainCategories) {
            recommendationService.getTopRated(category, 10);
        }
    }
}