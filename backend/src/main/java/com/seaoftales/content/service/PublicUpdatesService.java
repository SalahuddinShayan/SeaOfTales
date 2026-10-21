package com.seaoftales.content.service;

import com.seaoftales.content.dto.HomeChapterRow;
import com.seaoftales.content.dto.PublicHomeResponse;
import com.seaoftales.content.dto.PublicUpdatesResponse;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import org.springframework.data.domain.PageRequest;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PublicUpdatesService {
    private static final int MAX_PAGE_SIZE = 50;
    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;

    public PublicUpdatesService(ChapterRepository chapterRepository, NovelRepository novelRepository) {
        this.chapterRepository = chapterRepository;
        this.novelRepository = novelRepository;
    }

    public PublicUpdatesResponse getUpdates(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page must not be negative.");
        if (size < 1 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        var ids = chapterRepository.findLatestNovelUpdateIds(PageRequest.of(page, size));
        Map<Long, Novel> novels = novelRepository.findAllById(ids.getContent()).stream()
                .collect(Collectors.toMap(Novel::getId, Function.identity()));
        var grouped = ids.getContent().stream().map(novels::get).filter(java.util.Objects::nonNull).map(this::toUpdate).toList();
        return new PublicUpdatesResponse(grouped, page, size, ids.getTotalElements(), ids.getTotalPages());
    }

    private PublicHomeResponse.NovelUpdate toUpdate(Novel novel) {
        var chapters = chapterRepository.findHomeChaptersByNovelId(novel.getId(), PageRequest.of(0, 3));
        var summaries = chapters.stream().map(this::toSummary).toList();
        var latest = summaries.isEmpty() ? null : summaries.getFirst().updatedAt();
        return new PublicHomeResponse.NovelUpdate(toCard(novel), latest, summaries);
    }

    private PublicHomeResponse.ChapterSummary toSummary(HomeChapterRow chapter) {
        return new PublicHomeResponse.ChapterSummary(chapter.id(), chapter.novelId(), chapter.chapterNumber(), chapter.title(), chapter.updatedAt());
    }

    private PublicHomeResponse.NovelCard toCard(Novel novel) {
        String cover = novel.getCoverImage() == null || novel.getCoverImage().isBlank() ? null : "/api/novels/" + novel.getId() + "/cover";
        return new PublicHomeResponse.NovelCard(novel.getId(), novel.getTitle(), novel.getStatus().name(), novel.getSourceLanguage().name(), novel.getDescription(), cover);
    }
}
