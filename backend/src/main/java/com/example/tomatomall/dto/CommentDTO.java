package com.example.tomatomall.dto;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString
public class CommentDTO {
    private Integer id;
    private Integer productId;
    private Integer userId;
    private String username;
    private String userAvatar;
    private String content;
    private BigDecimal rating;
    private List<String> images;
    private LocalDateTime createTime;
    private Integer replyCount;
}