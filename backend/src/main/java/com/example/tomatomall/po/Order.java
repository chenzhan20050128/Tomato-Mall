package com.example.tomatomall.po;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId; // 订单ID

    @Column(nullable = false)
    private Long userId; // 用户ID

    @Column(nullable = false, name = "total_amount")
    private BigDecimal totalAmount; // 总金额

    @Column(nullable = false, name = "payment_method")
    private String paymentMethod; // 支付方式

    @Column(nullable = false)
    private String status = "PENDING";
    // 订单状态（PENDING, SUCCESS, FAILED, TIMEOUT）

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime; // 创建时间

    @Column(name = "trade_no")
    private String tradeNo; // 支付宝交易号

    @Column(name = "payment_time")
    private Timestamp paymentTime; // 支付时间
}