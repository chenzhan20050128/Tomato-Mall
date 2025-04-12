package com.example.tomatomall.config;/*
 * @date 04/11 17:28
 */

import com.example.tomatomall.dto.AdvertisementDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    /**
     * 配置JSON格式的RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, AdvertisementDTO> advertisementRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AdvertisementDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置Key的序列化器
        template.setKeySerializer(new StringRedisSerializer());

        // 设置Value的序列化器为JSON
        Jackson2JsonRedisSerializer<AdvertisementDTO> serializer =
                new Jackson2JsonRedisSerializer<>(AdvertisementDTO.class);
        template.setValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}