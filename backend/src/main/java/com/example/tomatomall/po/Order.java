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
    private Integer orderId; // 订单ID

    @Column(nullable = false)
    private Integer userId; // 用户ID

    @Column(nullable = false,name = "username")
    private String username; // 用户名 逆天67生成订单需要返回 好离谱 0409 19:54

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

    @Column(name = "lock_expire_time")
    private Timestamp lockExpireTime; // 库存锁定时间
}