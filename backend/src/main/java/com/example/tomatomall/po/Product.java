package com.example.tomatomall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "商品名称不能为空")
    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "商品价格不能为空")
    @Column(nullable = false)
    private BigDecimal price;

    @Column(length = 255)
    private String image;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean isAvailable = true;
}