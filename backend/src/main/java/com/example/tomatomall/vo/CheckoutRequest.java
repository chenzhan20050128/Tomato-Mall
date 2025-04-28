package com.example.tomatomall.vo;/*
 * @date 04/03 20:51
 */

import lombok.Data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class CheckoutRequest {
    private Integer userId;
    private List<Integer> cartItemIds;
    @JsonProperty("shipping_address")
    private ShippingAddress shippingAddress;
    @JsonProperty("payment_method")
    private String paymentMethod;
}