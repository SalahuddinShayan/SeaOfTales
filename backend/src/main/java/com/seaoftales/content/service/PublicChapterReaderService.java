package com.seaoftales.content.service;

import com.seaoftales.content.dto.PublicChapterReaderResponse;
import com.seaoftales.content.dto.PublicChapterReaderRow;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicChapterReaderService {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public PublicChapterReaderService(NovelRepository novelRepository, ChapterRepository chapterRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    public PublicChapterReaderResponse getChapter(Long novelId, Long chapterId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found."));
        List<PublicChapterReaderRow> chapters = chapterRepository.findPublicChapterReaderRowsByNovelId(novelId);
        int index = -1;
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).id().equals(chapterId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Chapter not found.");
        }

        PublicChapterReaderRow chapter = chapters.get(index);
        Long previousId = index > 0 ? chapters.get(index - 1).id() : null;
        Long nextId = index < chapters.size() - 1 ? chapters.get(index + 1).id() : null;
        return new PublicChapterReaderResponse(
                new PublicChapterReaderResponse.NovelDetails(novel.getId(), novel.getTitle()),
                new PublicChapterReaderResponse.ChapterDetails(
                        chapter.id(), chapter.novelId(), chapter.chapterNumber(),
                        chapter.title(), chapter.content(), chapter.updatedAt()),
                new PublicChapterReaderResponse.Navigation(previousId, nextId));
    }
}
