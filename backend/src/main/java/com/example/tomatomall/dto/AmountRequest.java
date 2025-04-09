package com.example.tomatomall.dto;

import lombok.Data;
@Data
public class AmountRequest {
    //cz 0408 22:29
    //用来解决PATCH 'http://127.0.0.1:8080/api/products/stockpile/13?amount=10'的问题
    private Integer amount;
}