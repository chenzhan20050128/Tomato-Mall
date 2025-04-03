package com.example.tomatomall.po;/*
 * @date 04/03 16:17
 */

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "carts_orders_relation")
@Data
public class CartOrderRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cartitem_id")
    private Integer cartItemId;

    @Column(name = "order_id")
    private Integer orderId;
}