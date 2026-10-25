package com.seaoftales.comment.repository;

import com.seaoftales.comment.entity.CommentReport;
import com.seaoftales.comment.enums.CommentReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    Optional<CommentReport> findByUserIdAndCommentId(Long userId, Long commentId);

    List<CommentReport> findByCommentIdIn(Collection<Long> commentIds);

    long countByCommentId(Long commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT report FROM CommentReport report WHERE report.id = :id")
    Optional<CommentReport> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            DELETE FROM CommentReport report WHERE report.commentId = :parentId
                OR report.commentId IN (SELECT comment.id FROM Comment comment WHERE comment.parentCommentId = :parentId)
            """)
    int deleteByParentOrReplyIds(@Param("parentId") Long parentId);

    @Modifying
    @Query("DELETE FROM CommentReport report WHERE report.commentId = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);

    Page<CommentReport> findByStatusOrderByCreatedAtAscIdAsc(CommentReportStatus status, Pageable pageable);
}
