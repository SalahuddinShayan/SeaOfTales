package com.seaoftales.reaction.repository;

import com.seaoftales.reaction.entity.Reaction;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    @Query(value = """
            SELECT reaction FROM Reaction reaction
            WHERE reaction.userId = :userId
              AND (
                (reaction.targetType = com.seaoftales.reaction.enums.ReactionTargetType.NOVEL
                  AND EXISTS (SELECT novel.id FROM Novel novel WHERE novel.id = reaction.targetId))
                OR
                (reaction.targetType = com.seaoftales.reaction.enums.ReactionTargetType.CHAPTER
                  AND EXISTS (
                    SELECT chapter.id FROM Chapter chapter JOIN Novel novel ON novel.id = chapter.novelId
                    JOIN ChapterEdition edition ON edition.chapterId = chapter.id
                    WHERE chapter.id = reaction.targetId
                      AND chapter.state = com.seaoftales.content.enums.ChapterState.EDITED
                  ))
              )
            """,
            countQuery = """
            SELECT COUNT(reaction) FROM Reaction reaction
            WHERE reaction.userId = :userId
              AND (
                (reaction.targetType = com.seaoftales.reaction.enums.ReactionTargetType.NOVEL
                  AND EXISTS (SELECT novel.id FROM Novel novel WHERE novel.id = reaction.targetId))
                OR
                (reaction.targetType = com.seaoftales.reaction.enums.ReactionTargetType.CHAPTER
                  AND EXISTS (
                    SELECT chapter.id FROM Chapter chapter JOIN Novel novel ON novel.id = chapter.novelId
                    JOIN ChapterEdition edition ON edition.chapterId = chapter.id
                    WHERE chapter.id = reaction.targetId
                      AND chapter.state = com.seaoftales.content.enums.ChapterState.EDITED
                  ))
              )
            """)
    Page<Reaction> findPublicActivityByUserId(@Param("userId") Long userId, Pageable pageable);

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
