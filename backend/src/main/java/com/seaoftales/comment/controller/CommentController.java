package com.seaoftales.comment.controller;

import com.seaoftales.comment.dto.CommentDtos;
import com.seaoftales.comment.enums.CommentReportCategory;
import com.seaoftales.comment.enums.CommentTargetType;
import com.seaoftales.comment.enums.CommentVoteType;
import com.seaoftales.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService comments;

    public CommentController(CommentService comments) { this.comments = comments; }

    @GetMapping("/{targetType}/{targetId}")
    public CommentDtos.CommentPage list(@PathVariable String targetType, @PathVariable Long targetId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        Authentication authentication) {
        return comments.list(parseTargetType(targetType), targetId, authenticatedName(authentication), page, size);
    }

    @GetMapping("/{targetType}/{targetId}/{parentCommentId}/replies")
    public CommentDtos.ReplyPage listReplies(@PathVariable String targetType, @PathVariable Long targetId,
                                             @PathVariable Long parentCommentId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             Authentication authentication) {
        return comments.listReplies(parseTargetType(targetType), targetId, parentCommentId,
                authenticatedName(authentication), page, size);
    }

    @PostMapping("/{targetType}/{targetId}")
    public ResponseEntity<CommentDtos.CommentView> create(@PathVariable String targetType, @PathVariable Long targetId,
                                                         @Valid @RequestBody CommentDtos.TextRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                comments.create(authentication.getName(), parseTargetType(targetType), targetId,
                        request == null ? null : request.body()));
    }

    @PostMapping("/{targetType}/{targetId}/{commentId}/replies")
    public ResponseEntity<CommentDtos.CommentView> reply(@PathVariable String targetType,
                                                         @PathVariable Long targetId,
                                                         @PathVariable Long commentId,
                                                         @Valid @RequestBody CommentDtos.TextRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comments.reply(authentication.getName(),
                parseTargetType(targetType), targetId, commentId, request == null ? null : request.body()));
    }

    @PatchMapping("/{commentId}")
    public CommentDtos.CommentView edit(@PathVariable Long commentId,
                                        @Valid @RequestBody CommentDtos.TextRequest request,
                                        Authentication authentication) {
        return comments.edit(authentication.getName(), commentId, request == null ? null : request.body());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId, Authentication authentication) {
        comments.delete(authentication.getName(), commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{commentId}/vote")
    public CommentDtos.VoteResult toggleVote(@PathVariable Long commentId,
                                             @RequestBody CommentDtos.VoteRequest request,
                                             Authentication authentication) {
        return comments.toggleVote(authentication.getName(), commentId, request == null ? null : request.voteType());
    }

    @DeleteMapping("/{commentId}/vote")
    public CommentDtos.VoteResult removeVote(@PathVariable Long commentId, Authentication authentication) {
        return comments.removeVote(authentication.getName(), commentId);
    }

    @PostMapping("/{commentId}/reports")
    public ResponseEntity<CommentDtos.ReportReceipt> report(@PathVariable Long commentId,
                                                            @RequestBody CommentDtos.ReportRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                comments.report(authentication.getName(), commentId, request == null ? null : request.category()));
    }

    private CommentTargetType parseTargetType(String raw) {
        try { return CommentTargetType.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException exception) {
            throw com.seaoftales.comment.service.CommentApiException.badRequest("Unsupported comment target type.");
        }
    }

    private String authenticatedName(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken
                ? null : authentication.getName();
    }
}
