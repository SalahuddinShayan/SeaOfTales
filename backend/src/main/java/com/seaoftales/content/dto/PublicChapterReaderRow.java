package com.seaoftales.content.dto;

import java.time.Instant;

public record PublicChapterReaderRow(
        Long id,
        Long novelId,
        String chapterNumber,
        String title,
        String content,
        Instant updatedAt) {
}
