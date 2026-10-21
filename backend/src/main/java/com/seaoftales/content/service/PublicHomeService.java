package com.seaoftales.content.service;

import com.seaoftales.content.dto.HomeChapterRow;
import com.seaoftales.content.dto.PublicHomeResponse;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicHomeService {

    private static final int FEATURED_LIMIT = 5;
    private static final int POPULAR_LIMIT = 4;
    private static final int LATEST_NOVEL_LIMIT = 10;
    private static final int RECENT_CHAPTER_LIMIT = 3;

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public PublicHomeService(NovelRepository novelRepository, ChapterRepository chapterRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    public PublicHomeResponse getHome() {
        return new PublicHomeResponse(featuredNovels(), popularNovels(), latestUpdates());
    }

    private List<PublicHomeResponse.NovelCard> featuredNovels() {
        List<Novel> novels = new ArrayList<>(novelRepository.findAll());
        Collections.shuffle(novels);
        return novels.stream()
                .limit(FEATURED_LIMIT)
                .map(this::toNovelCard)
                .toList();
    }

    private List<PublicHomeResponse.PopularNovel> popularNovels() {
        return novelRepository.findAllByOrderByCreatedAtAscIdAsc().stream()
                .limit(POPULAR_LIMIT)
                .map(novel -> {
                    List<HomeChapterRow> chapters = recentChapters(novel.getId(), 1);
                    PublicHomeResponse.ChapterSummary latestChapter = chapters.isEmpty()
                            ? null
                            : toChapterSummary(chapters.getFirst());
                    return new PublicHomeResponse.PopularNovel(toNovelCard(novel), latestChapter);
                })
                .toList();
    }

    private List<PublicHomeResponse.NovelUpdate> latestUpdates() {
        List<ChapterRepository.LatestNovelUpdate> updates = chapterRepository.findLatestNovelUpdates();
        List<Long> novelIds = updates.stream()
                .map(ChapterRepository.LatestNovelUpdate::getNovelId)
                .toList();
        Map<Long, Novel> novelsById = novelRepository.findAllById(novelIds).stream()
                .collect(Collectors.toMap(Novel::getId, Function.identity()));

        List<PublicHomeResponse.NovelUpdate> latestUpdates = new ArrayList<>();
        for (ChapterRepository.LatestNovelUpdate update : updates) {
            Novel novel = novelsById.get(update.getNovelId());
            if (novel == null) {
                continue;
            }

            List<PublicHomeResponse.ChapterSummary> chapters = recentChapters(novel.getId(), RECENT_CHAPTER_LIMIT)
                    .stream()
                    .map(this::toChapterSummary)
                    .toList();
            latestUpdates.add(new PublicHomeResponse.NovelUpdate(
                    toNovelCard(novel),
                    update.getLatestUpdatedAt(),
                    chapters));
            if (latestUpdates.size() == LATEST_NOVEL_LIMIT) {
                break;
            }
        }
        return latestUpdates;
    }

    private List<HomeChapterRow> recentChapters(Long novelId, int limit) {
        return chapterRepository.findHomeChaptersByNovelId(novelId, PageRequest.of(0, limit));
    }

    private PublicHomeResponse.NovelCard toNovelCard(Novel novel) {
        String coverImage = novel.getCoverImage() == null || novel.getCoverImage().isBlank()
                ? null
                : "/api/novels/" + novel.getId() + "/cover";
        return new PublicHomeResponse.NovelCard(
                novel.getId(),
                novel.getTitle(),
                novel.getStatus().name(),
                novel.getSourceLanguage().name(),
                novel.getDescription(),
                coverImage);
    }

    private PublicHomeResponse.ChapterSummary toChapterSummary(HomeChapterRow chapter) {
        return new PublicHomeResponse.ChapterSummary(
                chapter.id(),
                chapter.novelId(),
                chapter.chapterNumber(),
                chapter.title(),
                chapter.updatedAt());
    }
}