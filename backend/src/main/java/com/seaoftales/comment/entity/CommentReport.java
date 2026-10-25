package com.seaoftales.comment.entity;

import com.seaoftales.comment.enums.CommentReportCategory;
import com.seaoftales.comment.enums.CommentReportStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "comment_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_report_user_comment", columnNames = {"user_id", "comment_id"}),
        indexes = @Index(name = "ix_comment_report_status_created", columnList = "status,created_at,id"))
public class CommentReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 24)
    private CommentReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CommentReportStatus status = CommentReportStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolver_user_id")
    private Long resolverUserId;

    protected CommentReport() { }

    public CommentReport(Long userId, Long commentId, CommentReportCategory category) {
        this.userId = userId;
        this.commentId = commentId;
        this.category = category;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCommentId() { return commentId; }
    public CommentReportCategory getCategory() { return category; }
    public CommentReportStatus getStatus() { return status; }
    public void setStatus(CommentReportStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Long getResolverUserId() { return resolverUserId; }
    public void setResolution(Instant resolvedAt, Long resolverUserId) {
        this.resolvedAt = resolvedAt;
        this.resolverUserId = resolverUserId;
    }
}
