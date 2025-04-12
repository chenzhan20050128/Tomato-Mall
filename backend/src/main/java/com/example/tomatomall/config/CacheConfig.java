package com.example.tomatomall.config;/*
 * @date 04/11 18:34
 */

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    /**
     * 本地缓存配置（Caffeine）
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .initialCapacity(100)    // 初始容量
                .maximumSize(500)       // 最大缓存条目
                .expireAfterWrite(30, TimeUnit.SECONDS) // 写入后30秒过期
                .recordStats();         // 开启统计
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);
        manager.setCacheNames(Arrays.asList("advertisements", "other_cache"));
        return manager;
    }

}