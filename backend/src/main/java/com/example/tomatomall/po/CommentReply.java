package com.example.tomatomall.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comment_replies")
public class CommentReply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "comment_id", nullable = false)
    private Integer commentId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "reply_to_user_id", nullable = false)
    private Integer replyToUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    @Column(nullable = false)
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", insertable = false, updatable = false)
    @JsonIgnore
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_user_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account replyToUser;
}