package com.example.tomatomall.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;                // 评分ID

    @Column(name = "user_id", nullable = false)
    private Integer userId;           // 用户ID

    @Column(name = "product_id", nullable = false)
    private Integer productId;        // 商品ID

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal score;        // 评分值，0.0到5.0

    @Column(columnDefinition = "TEXT")
    private String comment;          // 文字评价

    @Column(name = "create_time", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime; // 评分创建时间

    @Transient
    private List<String> imageUrls;  // 评分图片URL列表（不在数据库中存储）
}