package com.seaoftales.identity.dto;

import com.seaoftales.comment.enums.CommentTargetType;

import java.time.Instant;

public record ProfileActivityComment(Long id, String body, Instant createdAt, Instant editedAt,
                                     CommentTargetType targetType, Long targetId, Long novelId,
                                     String novelTitle, String chapterNumber, String chapterTitle,
                                     Long parentCommentId, String parentAuthorUsername) { }
