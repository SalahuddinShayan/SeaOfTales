package com.seaoftales.content.service;

import com.seaoftales.content.entity.Chapter;
import com.seaoftales.content.entity.ChapterSource;
import com.seaoftales.content.enums.ChapterState;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.ChapterSourceRepository;
import com.seaoftales.content.repository.NovelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final ChapterSourceRepository chapterSourceRepository;
    private final NovelRepository novelRepository;

    public ChapterService(
            ChapterRepository chapterRepository,
            ChapterSourceRepository chapterSourceRepository,
            NovelRepository novelRepository) {
        this.chapterRepository = chapterRepository;
        this.chapterSourceRepository = chapterSourceRepository;
        this.novelRepository = novelRepository;
    }

    @Transactional
    public ChapterSource createChapterSource(
            Long novelId,
            String chapterNumber,
            String title,
            String content) {

        if (novelId == null || !novelRepository.existsById(novelId)) {
            throw new IllegalArgumentException("Novel not found.");
        }
        if (chapterNumber == null || chapterNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Chapter number is required.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Chapter title is required.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Source content is required.");
        }

        String normalizedChapterNumber = chapterNumber.trim();
        if (chapterRepository.existsByNovelIdAndChapterNumber(novelId, normalizedChapterNumber)) {
            throw new IllegalStateException("Chapter already exists for this novel.");
        }

        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setChapterNumber(normalizedChapterNumber);
        chapter.setState(ChapterState.UPLOADED);
        Chapter savedChapter = chapterRepository.save(chapter);

        ChapterSource chapterSource = new ChapterSource();
        chapterSource.setChapterId(savedChapter.getId());
        chapterSource.setTitle(title.trim());
        chapterSource.setContent(content);
        return chapterSourceRepository.save(chapterSource);
    }

    public List<Chapter> getByNovel(Long novelId) {
        return chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
    }

    public Chapter getById(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found."));
    }

    public ChapterSource getSource(Long chapterId) {
        return chapterSourceRepository.findByChapterId(chapterId)
                .orElseThrow(() -> new IllegalStateException("Chapter source not found."));
    }
}
