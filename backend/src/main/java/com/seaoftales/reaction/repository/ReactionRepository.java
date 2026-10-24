package com.seaoftales.reaction.repository;

import com.seaoftales.reaction.entity.Reaction;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(Long userId, ReactionTargetType targetType, Long targetId);

    @Query("""
            SELECT reaction.reactionType AS reactionType, COUNT(reaction) AS reactionCount
            FROM Reaction reaction
            WHERE reaction.targetType = :targetType AND reaction.targetId = :targetId
            GROUP BY reaction.reactionType
            """)
    List<ReactionCount> countByTarget(
            @Param("targetType") ReactionTargetType targetType,
            @Param("targetId") Long targetId);

    interface ReactionCount {
        ReactionType getReactionType();
        long getReactionCount();
    }
}
