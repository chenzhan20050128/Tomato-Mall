package com.example.tomatomall.service;

import com.example.tomatomall.dto.AdvertisementDTO;
import java.util.List;

public interface AdvertisementService {
    List<AdvertisementDTO> getAllAdvertisements();
    AdvertisementDTO updateAdvertisement(AdvertisementDTO advertisementDTO);
    AdvertisementDTO createAdvertisement(AdvertisementDTO advertisementDTO);
    void deleteAdvertisement(Integer id);
}