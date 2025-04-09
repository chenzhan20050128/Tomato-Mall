package com.example.tomatomall.dto;

import lombok.Data;

@Data
public class AdvertisementDTO {
    //67太过逆天，数据库中id是Integer类型，但是实际上Controller中传过来的id是String类型
    //cz 0408 21:20
    private String id;
    private String title;
    private String content;
    private String imgUrl;
    private String productId;
}
