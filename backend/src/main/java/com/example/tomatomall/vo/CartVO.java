package com.example.tomatomall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class CartVO {
    private List<CartItemVO> items;
    private int total;
    private BigDecimal totalAmount;

    public CartVO(List<CartItemVO> items, int total, BigDecimal totalAmount) {
        this.items = items;
        this.total = total;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public List<CartItemVO> getItems() {
        return items;
    }

    public void setItems(List<CartItemVO> items) {
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}