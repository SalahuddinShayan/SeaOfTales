package com.seaoftales.content.controller;

import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.service.NovelService;
import com.seaoftales.content.service.PublicNovelService;
import com.seaoftales.content.service.PublicChapterReaderService;

import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/novels")
public class NovelController {

    private final NovelService novelService;
    private final PublicNovelService publicNovelService;
    private final PublicChapterReaderService publicChapterReaderService;

    public NovelController(NovelService novelService, PublicNovelService publicNovelService, PublicChapterReaderService publicChapterReaderService) {
        this.novelService = novelService;
        this.publicNovelService = publicNovelService;
        this.publicChapterReaderService = publicChapterReaderService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Novel> createNovel(
        @RequestParam String title,
        @RequestParam String authorUsername,
        @RequestParam Language sourceLanguage,
        @RequestParam(required = false) String description,
        @RequestParam NovelStatus status,
        @RequestPart(required = false) MultipartFile coverImage
) throws IOException {

    Novel novel = new Novel();

    novel.setTitle(title);
    novel.setAuthorUsername(authorUsername);
    novel.setSourceLanguage(sourceLanguage);
    novel.setDescription(description);
    novel.setStatus(status);

    Novel createdNovel = novelService.createNovel(novel, coverImage);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdNovel);
}

    @GetMapping
    public ResponseEntity<List<Novel>> getAllNovels() {
        return ResponseEntity.ok(novelService.getAllNovels());
    }

    @GetMapping("/{novelId}/public")
    public ResponseEntity<?> getPublicNovel(@PathVariable Long novelId) {
        try {
            return ResponseEntity.ok(publicNovelService.getNovel(novelId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{novelId}/chapters/{chapterId}/public")
    public ResponseEntity<?> getPublicChapter(@PathVariable Long novelId, @PathVariable Long chapterId) {
        try {
            return ResponseEntity.ok(publicChapterReaderService.getChapter(novelId, chapterId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{novelId}/cover")
    public ResponseEntity<Resource> getCoverImage(@PathVariable Long novelId) {
        try {
            Resource image = novelService.getCoverImage(novelId);
            MediaType mediaType = MediaTypeFactory.getMediaType(image)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok().contentType(mediaType).body(image);
        } catch (NoSuchElementException | IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getNovelOptions() {
        Map<String, Object> options = new LinkedHashMap<>();

        options.put("sourceLanguages", Language.values());
        options.put("statuses", NovelStatus.values());

        return ResponseEntity.ok(options);
    }
}
