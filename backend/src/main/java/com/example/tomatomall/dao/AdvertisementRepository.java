package com.example.tomatomall.dao;

import com.example.tomatomall.po.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Integer> {
}