package com.seaoftales.comment.repository;

import com.seaoftales.comment.entity.CommentVote;
import com.seaoftales.comment.enums.CommentVoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
    Optional<CommentVote> findByUserIdAndCommentId(Long userId, Long commentId);

    List<CommentVote> findByUserIdAndCommentIdIn(Long userId, Collection<Long> commentIds);

    @Query("""
            SELECT vote.voteType AS voteType, COUNT(vote) AS voteCount
            FROM CommentVote vote
            WHERE vote.commentId = :commentId
            GROUP BY vote.voteType
            """)
    List<VoteCount> countByCommentId(@Param("commentId") Long commentId);

    @Query("""
            SELECT vote.commentId AS commentId, vote.voteType AS voteType, COUNT(vote) AS voteCount
            FROM CommentVote vote
            WHERE vote.commentId IN :commentIds
            GROUP BY vote.commentId, vote.voteType
            """)
    List<CommentVoteCount> countByCommentIdIn(@Param("commentIds") Collection<Long> commentIds);

    @Modifying
    @Query("""
            DELETE FROM CommentVote vote WHERE vote.commentId = :parentId
                OR vote.commentId IN (SELECT comment.id FROM Comment comment WHERE comment.parentCommentId = :parentId)
            """)
    int deleteByParentOrReplyIds(@Param("parentId") Long parentId);

    @Modifying
    @Query("DELETE FROM CommentVote vote WHERE vote.commentId = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);

    interface VoteCount {
        CommentVoteType getVoteType();
        long getVoteCount();
    }

    interface CommentVoteCount {
        Long getCommentId();
        CommentVoteType getVoteType();
        long getVoteCount();
    }
}
