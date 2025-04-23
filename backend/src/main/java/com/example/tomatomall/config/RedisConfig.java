package com.example.tomatomall.config;/*
 * @date 04/11 17:28
 */

import com.example.tomatomall.dto.AdvertisementDTO;
import com.example.tomatomall.po.Product;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.concurrent.TimeUnit;

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


    @Bean(name = "advertisementRedisTemplate")
    public RedisTemplate<String, AdvertisementDTO> advertisementRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AdvertisementDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用字符串序列化
        template.setKeySerializer(new StringRedisSerializer());

        // Value 使用 JSON 序列化
        Jackson2JsonRedisSerializer<AdvertisementDTO> serializer =
                new Jackson2JsonRedisSerializer<>(AdvertisementDTO.class);
        template.setValueSerializer(serializer);

        // Hash Key/Value 序列化
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    /**
     * 4.14 19:58
     * 配置用于存储单个 Product 对象的 RedisTemplate
     *
     * @param connectionFactory Redis连接工厂，由Spring自动注入
     * @return 配置好的RedisTemplate实例，用于操作String-Product类型的键值对
     */
    @Bean
    public RedisTemplate<String, Product> productRedisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate实例，指定键为String类型，值为Product类型
        RedisTemplate<String, Product> template = new RedisTemplate<>();

        // 设置Redis连接工厂，这是必须的配置
        template.setConnectionFactory(connectionFactory);

        // Key序列化配置：使用StringRedisSerializer，将键序列化为UTF-8格式的字符串
        // 这是Redis最常见和推荐的键序列化方式，保持键的可读性
        template.setKeySerializer(new StringRedisSerializer());

        // Value序列化配置：使用Jackson2JsonRedisSerializer将Product对象序列化为JSON
        // 指定了Product.class，确保序列化和反序列化时的类型安全
        Jackson2JsonRedisSerializer<Product> serializer = new Jackson2JsonRedisSerializer<>(Product.class);
        template.setValueSerializer(serializer);

        // 调用afterPropertiesSet()方法，确保所有属性设置完成后进行初始化
        // 这是Spring生命周期回调的一部分，确保模板正确配置
        template.afterPropertiesSet();

        return template;
    }


    @Bean
    public RedisTemplate<String, List<Product>> productListRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, List<Product>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }


}