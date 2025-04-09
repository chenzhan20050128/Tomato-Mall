// Stockpile.java
package com.example.tomatomall.po;

import lombok.Data;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

@Data
@Entity
@Table(name = "stockpiles")
public class Stockpile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer frozen;

    @Column(name = "locked_amount")
    private Integer lockedAmount = 0; // 新增锁定库存字段

    @Column(name = "lock_expire_time")
    private Timestamp lockExpireTime;
}