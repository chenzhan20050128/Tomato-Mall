package com.example.tomatomall.po;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "carts")
@Data
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cartItemId;

    private Integer userId;
    private Integer productId;
    private Integer quantity;
}