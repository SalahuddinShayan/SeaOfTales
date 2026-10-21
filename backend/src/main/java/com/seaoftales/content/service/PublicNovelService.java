package com.seaoftales.content.service;

import com.seaoftales.content.dto.PublicNovelResponse;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import org.springframework.stereotype.Service;

@Service
public class PublicNovelService {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public PublicNovelService(NovelRepository novelRepository, ChapterRepository chapterRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    public PublicNovelResponse getNovel(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("Novel not found."));
        String coverImage = novel.getCoverImage() == null || novel.getCoverImage().isBlank()
                ? null
                : "/api/novels/" + novel.getId() + "/cover";
        return new PublicNovelResponse(
                new PublicNovelResponse.NovelDetails(
                        novel.getId(), novel.getTitle(), novel.getAuthorUsername(),
                        novel.getSourceLanguage().name(), novel.getDescription(), coverImage,
                        novel.getStatus().name()),
                chapterRepository.findPublicChaptersByNovelId(novelId).stream()
                        .map(chapter -> new PublicNovelResponse.ChapterDetails(
                                chapter.id(), chapter.novelId(), chapter.chapterNumber(),
                                chapter.title(), chapter.updatedAt()))
                        .toList());
    }
}
