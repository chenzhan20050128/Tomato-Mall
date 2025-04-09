package com.example.tomatomall.controller;

import com.example.tomatomall.dto.AdvertisementDTO;
import com.example.tomatomall.service.AdvertisementService;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advertisements")
@Slf4j
public class AdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

    @GetMapping
    public Response<List<AdvertisementDTO>> getAllAdvertisements() {
        List<AdvertisementDTO> advertisements = advertisementService.getAllAdvertisements();
        return Response.buildSuccess(advertisements);
    }

    @PutMapping
    public Response<String> updateAdvertisement(@RequestBody AdvertisementDTO advertisementDTO) {
        try {
            AdvertisementDTO updatedAd = advertisementService.updateAdvertisement(advertisementDTO);
            return Response.buildSuccess("更新成功");
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        }
    }

    @PostMapping
    public Response<AdvertisementDTO> createAdvertisement(@RequestBody AdvertisementDTO advertisementDTO) {
        try {
            log.info("Creating advertisement: {}", advertisementDTO);
            AdvertisementDTO createdAd = advertisementService.createAdvertisement(advertisementDTO);
            log.info("Created advertisement: {}", createdAd.toString());
            return Response.buildSuccess(createdAd);
        } catch (RuntimeException e) {
            log.info("Create advertisement failed: {}", e.getMessage());
            return Response.buildFailure(e.getMessage(), "400");
        }
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteAdvertisement(@PathVariable String id) {
        try {
            advertisementService.deleteAdvertisement(Integer.parseInt(id));
            return Response.buildSuccess("删除成功");
        } catch (Exception e) {
            log.info("Delete advertisement failed: {}", e.getMessage());
            return Response.buildFailure("删除失败: " + e.getMessage(), "400");
        }
    }
}