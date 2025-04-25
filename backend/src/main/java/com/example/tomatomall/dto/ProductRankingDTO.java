package com.example.tomatomall.dto;/*
 * @date 04/17 14:43
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
// 修改ProductRankingDTO类
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRankingDTO {
    private Integer productId;
    private String title;
    private String cover;
    private Double price;
    private Integer sales;
    private Integer stock;
    private String description;
    private String category;
    private String author;
    private Double averageScore;
    private Integer ratingCount;

}