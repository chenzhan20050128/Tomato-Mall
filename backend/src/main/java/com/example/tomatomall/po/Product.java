package com.example.tomatomall.po;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Double rate;

    private String description;
    private String cover;
    private String detail;

    @JsonManagedReference
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Specification> specifications;

        public String toRagText() {
        return String.format("""
            Product ID: %d
            Title: %s
            Category: %s
            Description: %s
            Price: %.2f
            """,
                id, title, getCategory(), description, price.doubleValue());
    }

    // 从规格中提取分类（示例逻辑）
    private String getCategory() {
        return specifications.stream()
                .filter(s -> "category".equalsIgnoreCase(s.getItem()))
                .findFirst()
                .map(Specification::getValue)
                .orElse("General");
    }
}