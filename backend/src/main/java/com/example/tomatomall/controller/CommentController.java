package com.example.tomatomall.controller;

import com.example.tomatomall.dto.*;
import com.example.tomatomall.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 获取产品评论列表
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProductComments(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CommentDTO> comments = commentService.getProductComments(productId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("comments", comments.getContent());
        response.put("currentPage", comments.getNumber());
        response.put("totalItems", comments.getTotalElements());
        response.put("totalPages", comments.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 添加新评论
     */
    @PostMapping
    public ResponseEntity<?> addComment(
            @RequestBody CommentCreateDTO commentDTO,
            HttpServletRequest request) {

        // 直接从请求属性中获取用户ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录或登录已过期");
        }

        CommentDTO newComment = commentService.addComment(userId, commentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }

    /**
     * 获取评论的回复列表
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Map<String, Object>> getCommentReplies(
            @PathVariable Integer commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CommentReplyDTO> replies = commentService.getCommentReplies(commentId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("replies", replies.getContent());
        response.put("currentPage", replies.getNumber());
        response.put("totalItems", replies.getTotalElements());
        response.put("totalPages", replies.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 添加评论回复
     */
    @PostMapping("/replies")
    public ResponseEntity<?> addReply(
            @RequestBody CommentReplyCreateDTO replyDTO,
            HttpServletRequest request) {

        // 直接从请求属性中获取用户ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录或登录已过期");
        }

        CommentReplyDTO newReply = commentService.addReply(userId, replyDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newReply);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Integer commentId,
            HttpServletRequest request) {

        // 直接从请求属性中获取用户ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录或登录已过期");
        }

        boolean success = commentService.deleteComment(userId, commentId);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权删除此评论或评论不存在");
        }
    }

    /**
     * 删除评论回复
     */
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<?> deleteReply(
            @PathVariable Integer replyId,
            HttpServletRequest request) {

        // 直接从请求属性中获取用户ID
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录或登录已过期");
        }

        boolean success = commentService.deleteReply(userId, replyId);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权删除此回复或回复不存在");
        }
    }

    /**
     * 获取产品的平均评分
     */
    @GetMapping("/product/{productId}/rating")
    public ResponseEntity<BigDecimal> getProductAverageRating(
            @PathVariable Integer productId) {

        BigDecimal averageRating = commentService.getProductAverageRating(productId);
        return ResponseEntity.ok(averageRating);
    }
}