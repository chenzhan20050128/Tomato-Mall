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

    // Added by cz on 4.9 at 11:12
    public static CartOrderRelation of(Integer cartItemId, Integer orderId) {
        CartOrderRelation relation = new CartOrderRelation();
        relation.setCartItemId(cartItemId);
        relation.setOrderId(orderId);
        return relation;
    }
}