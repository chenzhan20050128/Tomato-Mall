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

    @Override
    public List<AdvertisementDTO> getAllAdvertisements() {
        List<Advertisement> advertisements = advertisementRepository.findAll();
        advertisements.sort(Comparator.comparing(Advertisement::getId).reversed());

        return advertisements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
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
        log.info("广告更新成功，ID: {}", updatedAd.getId());
        return convertToDTO(updatedAd);
    }

    @Override
    @Transactional
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
        log.info("广告创建成功，ID: {}", savedAd.getId());
        return convertToDTO(savedAd);
    }

    @Override
    @Transactional
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