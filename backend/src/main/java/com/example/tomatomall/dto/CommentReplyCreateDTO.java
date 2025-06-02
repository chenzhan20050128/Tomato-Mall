package com.example.tomatomall.dto;

import lombok.Data;

@Data
public class CommentReplyCreateDTO {
    private Integer commentId;
    private Integer replyToUserId;
    private String content;
}
