package com.example.tomatomall.vo;/*
 * @date 04/03 20:51
 */

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    private Integer userId;
    private List<Integer> cartItemIds;
    private ShippingAddress shippingAddress;
    private String paymentMethod;
}