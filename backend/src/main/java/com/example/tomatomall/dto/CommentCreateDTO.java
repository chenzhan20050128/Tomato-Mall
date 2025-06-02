package com.example.tomatomall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CommentCreateDTO {
    private Integer productId;
    private String content;
    private BigDecimal rating;
    private List<String> images;
}
