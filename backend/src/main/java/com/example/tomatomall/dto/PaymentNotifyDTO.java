package com.example.tomatomall.dto;/*
 * @date 04/11 10:33
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 允许忽略未知字段
public class PaymentNotifyDTO {
    @JsonProperty("out_trade_no")
    private String outTradeNo;

    @JsonProperty("trade_no")
    private String tradeNo;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
}