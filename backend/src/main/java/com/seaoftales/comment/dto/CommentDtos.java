package com.seaoftales.comment.dto;

import com.seaoftales.comment.enums.CommentReportCategory;
import com.seaoftales.comment.enums.CommentReportStatus;
import com.seaoftales.comment.enums.CommentTargetType;
import com.seaoftales.comment.enums.CommentVoteType;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CommentDtos {
    private CommentDtos() { }

    public record TextRequest(@NotBlank @Size(max = 3000) String body) { }
    public record VoteRequest(CommentVoteType voteType) { }
    public record ReportRequest(CommentReportCategory category) { }
    public record ReportStatusRequest(CommentReportStatus status) { }
    public record VoteResult(Long commentId, long likes, long dislikes, CommentVoteType currentUserVote) { }
    public record ReportReceipt(Long id, CommentReportStatus status, Instant createdAt) { }

    public record CommentPage(CommentTargetType targetType, Long targetId, List<CommentThread> content,
                              int page, int size, long totalElements, int totalPages) { }
    public record ReplyPage(Long parentCommentId, List<CommentView> content,
                            int page, int size, long totalElements, int totalPages) { }
    public record CommentThread(CommentView comment, long replyCount, List<CommentView> replies,
                                boolean hasMoreReplies) { }
    public record CommentView(Long id, String authorUsername, String profilePicture, String body,
                              Instant createdAt, Instant editedAt, long likes, long dislikes,
                              CommentVoteType currentUserVote) { }

    public record ReportPage(List<ReportItem> content, int page, int size, long totalElements, int totalPages) { }
    public record ReportItem(Long id, Long commentId, String reporterUsername, CommentReportCategory category,
                             CommentReportStatus status, Instant createdAt, String commentBody,
                             CommentTargetType targetType, Long targetId) { }
    public record ReportDetail(Long id, Long commentId, String reporterUsername, CommentReportCategory category,
                               CommentReportStatus status, Instant createdAt, Instant resolvedAt,
                               String resolverUsername, String commentBody, String commentAuthorUsername,
                               CommentTargetType targetType, Long targetId, Long parentCommentId) { }
    public record Error(String code, String message) { }
}
