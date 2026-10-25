package com.seaoftales.comment.entity;

import com.seaoftales.comment.enums.CommentTargetType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "ix_comment_target_created", columnList = "target_type,target_id,created_at,id"),
        @Index(name = "ix_comment_target_parent_created", columnList = "target_type,target_id,parent_comment_id,created_at,id"),
        @Index(name = "ix_comment_parent_created", columnList = "parent_comment_id,created_at,id"),
        @Index(name = "ix_comment_author_created", columnList = "author_user_id,created_at,id")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private CommentTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(nullable = false, length = 3000)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    protected Comment() { }

    public Comment(Long authorUserId, CommentTargetType targetType, Long targetId, Long parentCommentId, String body) {
        this.authorUserId = authorUserId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.parentCommentId = parentCommentId;
        this.body = body;
    }

    public Long getId() { return id; }
    public Long getAuthorUserId() { return authorUserId; }
    public CommentTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Long getParentCommentId() { return parentCommentId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }
}
