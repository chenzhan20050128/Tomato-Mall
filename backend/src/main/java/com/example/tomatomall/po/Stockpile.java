// Stockpile.java
package com.example.tomatomall.po;

import lombok.Data;
import javax.persistence.*;

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
}