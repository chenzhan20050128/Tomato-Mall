package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.AccountRepository;
import com.example.tomatomall.dao.CommentRepository;
import com.example.tomatomall.dao.CommentReplyRepository;
import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dto.*;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Comment;
import com.example.tomatomall.po.CommentReply;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReplyRepository commentReplyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DELETED = 2;

    @Override
    @Transactional
    public CommentDTO addComment(Integer userId, CommentCreateDTO commentDTO) {
        // 验证用户和商品
        Optional<Account> userOptional = accountRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        Account user = userOptional.get();

        Optional<Product> productOptional = productRepository.findById(commentDTO.getProductId());
        if (productOptional.isEmpty()) {
            throw new RuntimeException("商品不存在");
        }

        // 创建评论
        Comment comment = new Comment();
        comment.setProductId(commentDTO.getProductId());
        comment.setUserId(userId);
        comment.setContent(commentDTO.getContent());
        comment.setRating(commentDTO.getRating());
        comment.setImages(commentDTO.getImages() != null ? String.join(",", commentDTO.getImages()) : null);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setStatus(STATUS_ACTIVE);

        Comment savedComment = commentRepository.save(comment);
        logger.info("保存评论成功: {}", savedComment.getId());

        return convertToCommentDTO(savedComment, user);
    }

    @Override
    @Transactional
    public CommentReplyDTO addReply(Integer userId, CommentReplyCreateDTO replyDTO) {
        // 验证用户、评论和被回复用户
        Optional<Account> userOptional = accountRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        Account user = userOptional.get();

        Optional<Comment> commentOptional = commentRepository.findById(replyDTO.getCommentId());
        if (commentOptional.isEmpty()) {
            throw new RuntimeException("评论不存在");
        }

        Optional<Account> replyToUserOptional = accountRepository.findById(replyDTO.getReplyToUserId());
        if (replyToUserOptional.isEmpty()) {
            throw new RuntimeException("被回复的用户不存在");
        }
        Account replyToUser = replyToUserOptional.get();

        // 创建回复
        CommentReply reply = new CommentReply();
        reply.setCommentId(replyDTO.getCommentId());
        reply.setUserId(userId);
        reply.setReplyToUserId(replyDTO.getReplyToUserId());
        reply.setContent(replyDTO.getContent());
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        reply.setStatus(STATUS_ACTIVE);

        CommentReply savedReply = commentReplyRepository.save(reply);
        logger.info("保存回复成功: {}", savedReply.getId());

        return convertToReplyDTO(savedReply, user, replyToUser);
    }

    @Override
    public Page<CommentDTO> getProductComments(Integer productId, Pageable pageable) {
        logger.info("获取产品评论, productId={}, status={}", productId, STATUS_ACTIVE);

        // 先用计数方法验证数据是否存在
        int count = commentRepository.countCommentsByProductIdAndStatus(productId, STATUS_ACTIVE);
        logger.info("数据库中符合条件的评论数: {}", count);

        try {
            // 使用原生SQL查询
            Page<Comment> comments = commentRepository.findCommentsByProductIdAndStatusNative(
                    productId, STATUS_ACTIVE, pageable);

            logger.info("查询结果: 总条数={}, 当前页条数={}",
                    comments.getTotalElements(), comments.getContent().size());

            if (!comments.hasContent()) {
                logger.warn("未找到产品评论数据");
                return Page.empty(pageable);
            }

            return comments.map(comment -> {
                // 查询用户信息
                Optional<Account> userOpt = accountRepository.findById(comment.getUserId());
                return convertToCommentDTO(comment, userOpt.orElse(null));
            });
        } catch (Exception e) {
            logger.error("获取产品评论异常: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<CommentReplyDTO> getCommentReplies(Integer commentId, Pageable pageable) {
        logger.info("获取评论回复, commentId={}", commentId);

        try {
            Page<CommentReply> replies = commentReplyRepository.findByCommentIdAndStatusOrderByCreateTimeAsc(
                    commentId, STATUS_ACTIVE, pageable);

            logger.info("回复查询结果: 总条数={}", replies.getTotalElements());

            List<CommentReplyDTO> replyDTOs = replies.getContent().stream()
                    .map(reply -> {
                        try {
                            Optional<Account> userOptional = accountRepository.findById(reply.getUserId());
                            Optional<Account> replyToUserOptional = accountRepository.findById(reply.getReplyToUserId());

                            Account user = userOptional.orElse(null);
                            Account replyToUser = replyToUserOptional.orElse(null);

                            return convertToReplyDTO(reply, user, replyToUser);
                        } catch (Exception e) {
                            logger.error("转换回复DTO时出错: replyId={}, error={}", reply.getId(), e.getMessage());
                            CommentReplyDTO basicDTO = new CommentReplyDTO();
                            basicDTO.setId(reply.getId());
                            basicDTO.setCommentId(reply.getCommentId());
                            basicDTO.setContent(reply.getContent());
                            basicDTO.setUserId(reply.getUserId());
                            basicDTO.setReplyToUserId(reply.getReplyToUserId());
                            basicDTO.setCreateTime(reply.getCreateTime());
                            return basicDTO;
                        }
                    })
                    .collect(Collectors.toList());

            return new PageImpl<>(replyDTOs, pageable, replies.getTotalElements());
        } catch (Exception e) {
            logger.error("获取评论回复时发生错误: {}", e.getMessage(), e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }

    @Override
    public BigDecimal getProductAverageRating(Integer productId) {
        logger.info("计算产品平均评分, productId={}", productId);
        try {
            List<BigDecimal> ratings = commentRepository.findRatingsByProductIdAndStatus(productId, STATUS_ACTIVE);

            if (ratings.isEmpty()) {
                return BigDecimal.ZERO;
            }

            BigDecimal sum = ratings.stream()
                    .filter(r -> r != null) // 过滤可能的null值
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return sum.divide(new BigDecimal(ratings.size()), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            logger.error("计算产品平均评分时发生错误: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public boolean deleteComment(Integer userId, Integer commentId) {
        try {
            Optional<Comment> commentOptional = commentRepository.findById(commentId);
            if (commentOptional.isEmpty()) {
                logger.warn("要删除的评论不存在: commentId={}", commentId);
                return false;
            }

            Comment comment = commentOptional.get();
            if (!comment.getUserId().equals(userId)) {
                logger.warn("无权删除此评论: commentId={}, userId={}, commentUserId={}",
                        commentId, userId, comment.getUserId());
                return false;
            }

            comment.setStatus(STATUS_DELETED);
            comment.setUpdateTime(LocalDateTime.now());
            commentRepository.save(comment);
            logger.info("评论已标记为删除: commentId={}", commentId);

            // 同时删除所有回复
            commentReplyRepository.updateStatusByCommentId(commentId, STATUS_DELETED);
            logger.info("评论相关回复已标记为删除: commentId={}", commentId);

            return true;
        } catch (Exception e) {
            logger.error("删除评论时发生错误: commentId={}, error={}", commentId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteReply(Integer userId, Integer replyId) {
        try {
            Optional<CommentReply> replyOptional = commentReplyRepository.findById(replyId);
            if (replyOptional.isEmpty()) {
                logger.warn("要删除的回复不存在: replyId={}", replyId);
                return false;
            }

            CommentReply reply = replyOptional.get();
            if (!reply.getUserId().equals(userId)) {
                logger.warn("无权删除此回复: replyId={}, userId={}, replyUserId={}",
                        replyId, userId, reply.getUserId());
                return false;
            }

            reply.setStatus(STATUS_DELETED);
            reply.setUpdateTime(LocalDateTime.now());
            commentReplyRepository.save(reply);
            logger.info("回复已标记为删除: replyId={}", replyId);

            return true;
        } catch (Exception e) {
            logger.error("删除回复时发生错误: replyId={}, error={}", replyId, e.getMessage(), e);
            return false;
        }
    }

    // 辅助方法：转换为评论DTO
    private CommentDTO convertToCommentDTO(Comment comment, Account user) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setProductId(comment.getProductId());
        dto.setUserId(comment.getUserId());

        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setUserAvatar(user.getAvatar());
        } else {
            // 避免空指针，设置默认值
            dto.setUsername("未知用户");
            dto.setUserAvatar(null);
            logger.warn("评论用户信息未找到: commentId={}, userId={}", comment.getId(), comment.getUserId());
        }

        dto.setContent(comment.getContent());
        dto.setRating(comment.getRating());

        if (comment.getImages() != null && !comment.getImages().isEmpty()) {
            dto.setImages(List.of(comment.getImages().split(",")));
        } else {
            dto.setImages(new ArrayList<>());
        }

        dto.setCreateTime(comment.getCreateTime());

        // 获取有效回复数量
        try {
            int replyCount = commentReplyRepository.countByCommentIdAndStatus(comment.getId(), STATUS_ACTIVE);
            dto.setReplyCount(replyCount);
        } catch (Exception e) {
            logger.error("获取评论回复数量时出错: commentId={}, error={}", comment.getId(), e.getMessage());
            dto.setReplyCount(0); // 出错时设为0
        }

        return dto;
    }

    // 辅助方法：转换为回复DTO
    private CommentReplyDTO convertToReplyDTO(CommentReply reply, Account user, Account replyToUser) {
        CommentReplyDTO dto = new CommentReplyDTO();
        dto.setId(reply.getId());
        dto.setCommentId(reply.getCommentId());
        dto.setUserId(reply.getUserId());

        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setUserAvatar(user.getAvatar());
        } else {
            dto.setUsername("未知用户");
        }

        dto.setReplyToUserId(reply.getReplyToUserId());

        if (replyToUser != null) {
            dto.setReplyToUsername(replyToUser.getUsername());
        } else {
            dto.setReplyToUsername("未知用户");
        }

        dto.setContent(reply.getContent());
        dto.setCreateTime(reply.getCreateTime());

        return dto;
    }
}