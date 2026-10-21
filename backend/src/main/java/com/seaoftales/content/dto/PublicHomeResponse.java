package com.seaoftales.content.dto;

import java.time.Instant;
import java.util.List;

public record PublicHomeResponse(
        List<NovelCard> featuredNovels,
        List<PopularNovel> popularNovels,
        List<NovelUpdate> latestUpdates) {

    public record NovelCard(
            Long id,
            String title,
            String status,
            String sourceLanguage,
            String description,
            String coverImage) {
    }

    public record ChapterSummary(
            Long id,
            Long novelId,
            String chapterNumber,
            String title,
            Instant updatedAt) {
    }

    public record PopularNovel(NovelCard novel, ChapterSummary latestChapter) {
    }

    public record NovelUpdate(
            NovelCard novel,
            Instant latestUpdatedAt,
            List<ChapterSummary> recentChapters) {
    }
}