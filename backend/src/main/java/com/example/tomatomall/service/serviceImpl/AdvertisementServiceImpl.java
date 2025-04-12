package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.AdvertisementRepository;
import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dto.AdvertisementDTO;
import com.example.tomatomall.po.Advertisement;
import com.example.tomatomall.service.AdvertisementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdvertisementServiceImpl implements AdvertisementService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private ProductRepository productRepository;


    @Autowired
    @Qualifier("advertisementRedisTemplate") // 指定Bean名称
    private RedisTemplate<String, AdvertisementDTO> redisTemplate;
    private static final String CACHE_KEY = "advertisements";

    @Override
    @Cacheable(cacheNames = "advertisements", key = "'all_ads'", sync = true) //先从本地Caffeine缓存获取数据 30秒过期
    public List<AdvertisementDTO> getAllAdvertisements() {

        // 从缓存获取数据 1小时过期
        List<AdvertisementDTO> cached = redisTemplate.opsForList().range(CACHE_KEY, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            log.info("从Redis缓存中获取广告数据 数据个数:{}", cached.size());
            return cached;
        }

        // 缓存未命中则查询数据库
        List<Advertisement> ads = advertisementRepository.findAll();
        List<AdvertisementDTO> dtos = ads.stream()
                .sorted(Comparator.comparing(Advertisement::getId).reversed())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("从数据库中获取广告数据 数据个数:{}", dtos.size());

        // 写入缓存
        if (!dtos.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(CACHE_KEY, dtos);
            redisTemplate.expire(CACHE_KEY, 1, TimeUnit.HOURS);
        }
        return dtos;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "advertisements",key = "'all_ads'", allEntries = true)
    public AdvertisementDTO updateAdvertisement(AdvertisementDTO advertisementDTO) {
        // 验证商品存在
        Integer productId = Integer.parseInt(advertisementDTO.getProductId());
        boolean productExists = productRepository.findById(productId).isPresent();
        if (!productExists) {
            throw new RuntimeException("商品不存在");
        }

        // 验证广告存在
        Integer adId = Integer.parseInt(advertisementDTO.getId());
        Optional<Advertisement> existingAdOpt = advertisementRepository.findById(adId);
        if (!existingAdOpt.isPresent()) {
            throw new RuntimeException("广告不存在");
        }

        Advertisement existingAd = existingAdOpt.get();

        // 更新非null字段
        if (advertisementDTO.getTitle() != null) {
            existingAd.setTitle(advertisementDTO.getTitle());
        }
        if (advertisementDTO.getContent() != null) {
            existingAd.setContent(advertisementDTO.getContent());
        }
        if (advertisementDTO.getImgUrl() != null) {
            existingAd.setImageUrl(advertisementDTO.getImgUrl());
        }
        existingAd.setProductId(productId);

        Advertisement updatedAd = advertisementRepository.save(existingAd);
        redisTemplate.delete(CACHE_KEY);
        log.info("广告更新成功，ID: {}", updatedAd.getId());
        return convertToDTO(updatedAd);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "advertisements",key = "'all_ads'", allEntries = true)
    public AdvertisementDTO createAdvertisement(AdvertisementDTO advertisementDTO) {
        // 验证商品存在
        Integer productId = Integer.parseInt(advertisementDTO.getProductId());
        boolean productExists = productRepository.findById(productId).isPresent();
        if (!productExists) {
            throw new RuntimeException("商品不存在");
        }

        Advertisement advertisement = new Advertisement();
        advertisement.setTitle(advertisementDTO.getTitle());
        advertisement.setContent(advertisementDTO.getContent());
        advertisement.setImageUrl(advertisementDTO.getImgUrl());
        advertisement.setProductId(productId);

        Advertisement savedAd = advertisementRepository.save(advertisement);
        redisTemplate.delete(CACHE_KEY);
        log.info("广告创建成功，ID: {}", savedAd.getId());
        return convertToDTO(savedAd);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "advertisements",key = "'all_ads'", allEntries = true)
    public void deleteAdvertisement(Integer id) {
        advertisementRepository.deleteById(id);
        log.info("广告删除成功，ID: {}", id);
    }

    private AdvertisementDTO convertToDTO(Advertisement advertisement) {
        AdvertisementDTO dto = new AdvertisementDTO();
        dto.setId(advertisement.getId().toString());
        dto.setTitle(advertisement.getTitle());
        dto.setContent(advertisement.getContent());
        dto.setImgUrl(advertisement.getImageUrl());
        dto.setProductId(advertisement.getProductId().toString());//将原本Integer的内容转化成String
        return dto;
    }
}