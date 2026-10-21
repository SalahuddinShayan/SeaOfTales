package com.seaoftales.content.controller;

import com.seaoftales.content.entity.ChapterSource;
import com.seaoftales.content.entity.Chapter;
import com.seaoftales.content.service.ChapterPromptService;
import com.seaoftales.content.service.ChapterService;
import com.seaoftales.content.repository.ChapterTranslationRepository;
import com.seaoftales.content.repository.ChapterEditionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChapterController {

    private final ChapterService chapterService;
    private final ChapterPromptService chapterPromptService;
    private final ChapterTranslationRepository chapterTranslationRepository;
    private final ChapterEditionRepository chapterEditionRepository;

    public ChapterController(ChapterService chapterService, ChapterPromptService chapterPromptService, ChapterTranslationRepository chapterTranslationRepository, ChapterEditionRepository chapterEditionRepository) {
        this.chapterService = chapterService;
        this.chapterPromptService = chapterPromptService;
        this.chapterTranslationRepository = chapterTranslationRepository;
        this.chapterEditionRepository = chapterEditionRepository;
    }

    @GetMapping("/novels/{novelId}/chapters")
    public ResponseEntity<?> getByNovel(@PathVariable Long novelId) {
        return ResponseEntity.ok(chapterService.getByNovel(novelId).stream().map(chapter -> {
            ChapterSource source = chapterService.getSource(chapter.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", chapter.getId());
            item.put("novelId", chapter.getNovelId());
            item.put("chapterNumber", chapter.getChapterNumber());
            var edition = chapterEditionRepository.findByChapterId(chapter.getId()).orElse(null);
            item.put("title", chapter.getState().name().equals("EDITED") && edition != null && edition.getTitle() != null && !edition.getTitle().isBlank() ? edition.getTitle() : source.getTitle());
            item.put("state", chapter.getState());
            item.put("translationExists", chapterTranslationRepository.findByChapterId(chapter.getId()).isPresent());
            item.put("createdAt", chapter.getCreatedAt());
            item.put("updatedAt", chapter.getUpdatedAt());
            return item;
        }).toList());
    }

    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<?> getById(@PathVariable Long chapterId) {
        try {
            Chapter chapter = chapterService.getById(chapterId);
            ChapterSource source = chapterService.getSource(chapterId);
            return ResponseEntity.ok(chapterDetails(chapter, source));
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalStateException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    @PostMapping("/chapters/{chapterId}/prompt")
    public ResponseEntity<?> buildPrompt(@PathVariable Long chapterId) {
        try {
            ChapterPromptService.PromptBuildResult result = chapterPromptService.buildPrompt(chapterId);
            Map<String, Object> response = chapterDetails(result.chapter(), result.source());
            response.put("prompt", result.prompt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalStateException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    private Map<String, Object> chapterDetails(Chapter chapter, ChapterSource source) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", chapter.getId());
        details.put("chapterId", chapter.getId());
        details.put("novelId", chapter.getNovelId());
        details.put("chapterNumber", chapter.getChapterNumber());
        details.put("title", source.getTitle());
        details.put("state", chapter.getState());
        details.put("createdAt", chapter.getCreatedAt());
        details.put("updatedAt", chapter.getUpdatedAt());
        return details;
    }

    @PostMapping("/chapters")
    public ResponseEntity<?> createChapter(@RequestBody ChapterIngestionRequest request) {
        try {
            ChapterSource source = chapterService.createChapterSource(
                    request.novelId(),
                    request.chapterNumber(),
                    request.title(),
                    request.content());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("chapterId", source.getChapterId());
            response.put("novelId", request.novelId());
            response.put("chapterNumber", request.chapterNumber().trim());
            response.put("title", source.getTitle());
            response.put("state", "UPLOADED");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }

    public record ChapterIngestionRequest(
            Long novelId,
            String chapterNumber,
            String title,
            String content) {
    }
}
