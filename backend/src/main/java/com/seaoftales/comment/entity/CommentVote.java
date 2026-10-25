package com.seaoftales.comment.entity;

import com.seaoftales.comment.enums.CommentVoteType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "comment_votes",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_vote_user_comment", columnNames = {"user_id", "comment_id"}),
        indexes = @Index(name = "ix_comment_vote_comment", columnList = "comment_id"))
public class CommentVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 10)
    private CommentVoteType voteType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CommentVote() { }

    public CommentVote(Long userId, Long commentId, CommentVoteType voteType) {
        this.userId = userId;
        this.commentId = commentId;
        this.voteType = voteType;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCommentId() { return commentId; }
    public CommentVoteType getVoteType() { return voteType; }
    public void setVoteType(CommentVoteType voteType) { this.voteType = voteType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
