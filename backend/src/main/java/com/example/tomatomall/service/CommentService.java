package com.example.tomatomall.service;

import com.example.tomatomall.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CommentService {
    CommentDTO addComment(Integer userId, CommentCreateDTO commentDTO);
    CommentReplyDTO addReply(Integer userId, CommentReplyCreateDTO replyDTO);
    Page<CommentDTO> getProductComments(Integer productId, Pageable pageable);
    Page<CommentReplyDTO> getCommentReplies(Integer commentId, Pageable pageable);
    BigDecimal getProductAverageRating(Integer productId);
    boolean deleteComment(Integer userId, Integer commentId);
    boolean deleteReply(Integer userId, Integer replyId);
}