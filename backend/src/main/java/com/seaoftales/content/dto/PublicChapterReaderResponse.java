package com.seaoftales.content.dto;

import java.time.Instant;

public record PublicChapterReaderResponse(
        NovelDetails novel,
        ChapterDetails chapter,
        Navigation navigation) {

    public record NovelDetails(Long id, String title) {
    }

    public record ChapterDetails(
            Long id,
            Long novelId,
            String chapterNumber,
            String title,
            String content,
            Instant updatedAt) {
    }

    public record Navigation(Long previousChapterId, Long nextChapterId) {
    }
}
