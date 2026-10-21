package com.seaoftales.content.dto;

import java.time.Instant;
import java.util.List;

public record PublicNovelResponse(
        NovelDetails novel,
        List<ChapterDetails> chapters) {

    public record NovelDetails(
            Long id,
            String title,
            String authorUsername,
            String sourceLanguage,
            String description,
            String coverImage,
            String status) {
    }

    public record ChapterDetails(
            Long id,
            Long novelId,
            String chapterNumber,
            String title,
            Instant updatedAt) {
    }
}
