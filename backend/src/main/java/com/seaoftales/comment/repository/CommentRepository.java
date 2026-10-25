package com.seaoftales.comment.repository;

import com.seaoftales.comment.entity.Comment;
import com.seaoftales.comment.enums.CommentTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query(value = """
            SELECT comment FROM Comment comment
            WHERE comment.authorUserId = :authorId
              AND (
                (comment.targetType = com.seaoftales.comment.enums.CommentTargetType.NOVEL
                  AND EXISTS (SELECT novel.id FROM Novel novel WHERE novel.id = comment.targetId))
                OR
                (comment.targetType = com.seaoftales.comment.enums.CommentTargetType.CHAPTER
                  AND EXISTS (
                    SELECT chapter.id FROM Chapter chapter JOIN Novel novel ON novel.id = chapter.novelId
                    JOIN ChapterEdition edition ON edition.chapterId = chapter.id
                    WHERE chapter.id = comment.targetId
                      AND chapter.state = com.seaoftales.content.enums.ChapterState.EDITED
                  ))
              )
            """,
            countQuery = """
            SELECT COUNT(comment) FROM Comment comment
            WHERE comment.authorUserId = :authorId
              AND (
                (comment.targetType = com.seaoftales.comment.enums.CommentTargetType.NOVEL
                  AND EXISTS (SELECT novel.id FROM Novel novel WHERE novel.id = comment.targetId))
                OR
                (comment.targetType = com.seaoftales.comment.enums.CommentTargetType.CHAPTER
                  AND EXISTS (
                    SELECT chapter.id FROM Chapter chapter JOIN Novel novel ON novel.id = chapter.novelId
                    JOIN ChapterEdition edition ON edition.chapterId = chapter.id
                    WHERE chapter.id = comment.targetId
                      AND chapter.state = com.seaoftales.content.enums.ChapterState.EDITED
                  ))
              )
            """)
    Page<Comment> findPublicActivityByAuthorUserId(@Param("authorId") Long authorId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT comment FROM Comment comment WHERE comment.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") Long id);

    org.springframework.data.domain.Page<Comment> findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtAscIdAsc(
            CommentTargetType targetType, Long targetId, Pageable pageable);

    List<Comment> findByTargetTypeAndTargetIdAndParentCommentIdIsNotNullOrderByCreatedAtAscIdAsc(
            CommentTargetType targetType, Long targetId, Pageable pageable);

    org.springframework.data.domain.Page<Comment> findByParentCommentIdOrderByCreatedAtAscIdAsc(Long parentCommentId, Pageable pageable);

    List<Comment> findByParentCommentIdInOrderByParentCommentIdAscCreatedAtAscIdAsc(
            Collection<Long> parentCommentIds, Pageable pageable);

    @Query("""
            SELECT comment.parentCommentId AS parentCommentId, COUNT(comment) AS replyCount
            FROM Comment comment WHERE comment.parentCommentId IN :parentIds
            GROUP BY comment.parentCommentId
            """)
    List<ReplyCount> countRepliesByParentCommentIdIn(@Param("parentIds") Collection<Long> parentCommentIds);

    boolean existsByAuthorUserIdAndTargetTypeAndTargetIdAndBody(
            Long authorUserId, CommentTargetType targetType, Long targetId, String body);

    boolean existsByAuthorUserIdAndTargetTypeAndTargetIdAndBodyAndIdNot(
            Long authorUserId, CommentTargetType targetType, Long targetId, String body, Long excludedId);

    java.util.Optional<Comment> findFirstByAuthorUserIdOrderByCreatedAtDescIdDesc(Long authorUserId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Comment comment WHERE comment.parentCommentId = :parentId")
    int deleteRepliesByParentId(@Param("parentId") Long parentId);

    @Query("""
            SELECT comment FROM Comment comment
            WHERE comment.targetType = :targetType AND comment.targetId = :targetId
            ORDER BY comment.createdAt ASC, comment.id ASC
            """)
    List<Comment> findTargetComments(@Param("targetType") CommentTargetType targetType,
                                     @Param("targetId") Long targetId, Pageable pageable);

    interface ReplyCount {
        Long getParentCommentId();
        long getReplyCount();
    }
}
