package com.example.tomatomall.vo;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class CartItemVO {
    private Integer cartItemId;
    private Integer productId;
    private String title;
    private BigDecimal price;
    private String description;
    private String cover;
    private String detail;
    private Integer quantity;

}