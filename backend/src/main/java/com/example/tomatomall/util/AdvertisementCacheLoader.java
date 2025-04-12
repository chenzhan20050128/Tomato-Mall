package com.example.tomatomall.util;

import com.example.tomatomall.dao.AdvertisementRepository;
import com.example.tomatomall.dto.AdvertisementDTO;
import com.example.tomatomall.po.Advertisement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class AdvertisementCacheLoader implements CommandLineRunner {
    private static final String CACHE_KEY = "advertisements";

    @Autowired
    private AdvertisementRepository advertisementRepository;
    @Autowired
    private RedisTemplate<String, AdvertisementDTO> redisTemplate;

    @Override
    public void run(String... args) {
        // 应用启动时加载全部广告数据到Redis
        List<Advertisement> ads = advertisementRepository.findAll();
        List<AdvertisementDTO> dtos = ads.stream()
                .sorted(Comparator.comparing(Advertisement::getId).reversed())
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        redisTemplate.delete(CACHE_KEY);
        if (!dtos.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(CACHE_KEY, dtos);
            redisTemplate.expire(CACHE_KEY, 1, TimeUnit.HOURS);
        }
    }

    private AdvertisementDTO convertToDTO(Advertisement ad) {
        AdvertisementDTO dto = new AdvertisementDTO();
        dto.setId(ad.getId().toString());
        dto.setTitle(ad.getTitle());
        dto.setContent(ad.getContent());
        dto.setImgUrl(ad.getImageUrl());
        dto.setProductId(ad.getProductId().toString());
        return dto;
    }
}