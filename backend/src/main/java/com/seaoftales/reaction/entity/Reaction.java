package com.seaoftales.reaction.entity;

import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reaction_user_target",
                columnNames = {"user_id", "target_type", "target_id"}),
        indexes = @Index(name = "ix_reaction_target", columnList = "target_type,target_id"))
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private ReactionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 16)
    private ReactionType reactionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Reaction() {}

    public Reaction(Long userId, ReactionTargetType targetType, Long targetId, ReactionType reactionType) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reactionType = reactionType;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ReactionTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }
    public Instant getCreatedAt() { return createdAt; }
}
