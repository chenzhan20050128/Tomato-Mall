package com.example.tomatomall.vo;/*
 * @date 04/03 20:51
 */

import lombok.Data;

@Data
public class ShippingAddress {
    private String recipientName;
    private String phone;
    private String postalCode;
    private String addressDetail;
}