package com.seaoftales.content.dto;

import java.time.Instant;

public record PublicNovelChapterRow(
        Long id,
        Long novelId,
        String chapterNumber,
        String title,
        Instant updatedAt) {
}
