package com.example.tomatomall.dao;

import com.example.tomatomall.po.CommentReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReplyRepository extends JpaRepository<CommentReply, Integer> {
    Page<CommentReply> findByCommentIdAndStatusOrderByCreateTimeAsc(Integer commentId, Integer status, Pageable pageable);

    int countByCommentIdAndStatus(Integer commentId, Integer status);

    @Modifying
    @Query("UPDATE CommentReply cr SET cr.status = :status, cr.updateTime = CURRENT_TIMESTAMP WHERE cr.commentId = :commentId")
    void updateStatusByCommentId(@Param("commentId") Integer commentId, @Param("status") Integer status);
}