package com.seaoftales.comment.controller;

import com.seaoftales.comment.dto.CommentDtos;
import com.seaoftales.comment.enums.CommentReportStatus;
import com.seaoftales.comment.service.CommentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/admin/comment-reports")
public class AdminCommentReportController {
    private final CommentService comments;

    public AdminCommentReportController(CommentService comments) { this.comments = comments; }

    @GetMapping
    public CommentDtos.ReportPage list(@RequestParam(defaultValue = "PENDING") CommentReportStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return comments.listReports(status, page, size);
    }

    @GetMapping("/{reportId}")
    public CommentDtos.ReportDetail get(@PathVariable Long reportId) { return comments.getReport(reportId); }

    @PatchMapping("/{reportId}")
    public CommentDtos.ReportDetail resolve(@PathVariable Long reportId,
                                            @RequestBody CommentDtos.ReportStatusRequest request,
                                            Authentication authentication) {
        return comments.resolveReport(authentication.getName(), reportId, request == null ? null : request.status());
    }
}
