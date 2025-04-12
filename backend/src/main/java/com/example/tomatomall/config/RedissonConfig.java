package com.example.tomatomall.config;/*
 * @date 04/12 10:48
 */

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单节点模式，格式：redis://127.0.0.1:6379
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(64)    // 默认64
                .setConnectionMinimumIdleSize(24); // 默认24
        return Redisson.create(config);
    }
}