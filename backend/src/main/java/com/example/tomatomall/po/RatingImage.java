package com.example.tomatomall.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rating_images")
public class RatingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;        // 图片ID

    @Column(name = "rating_id", nullable = false)
    private Integer ratingId; // 评分ID

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;  // 图片URL
}