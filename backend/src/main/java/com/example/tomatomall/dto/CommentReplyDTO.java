package com.example.tomatomall.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentReplyDTO {
    private Integer id;
    private Integer commentId;
    private Integer userId;
    private String username;
    private String userAvatar;
    private Integer replyToUserId;
    private String replyToUsername;
    private String content;
    private LocalDateTime createTime;
}
