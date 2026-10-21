package com.seaoftales.content.controller;

import com.seaoftales.content.dto.NovelCatalogResponse;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.service.PublicNovelCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/novels/catalog")
public class PublicNovelCatalogController {

    private final PublicNovelCatalogService catalogService;

    public PublicNovelCatalogController(PublicNovelCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<NovelCatalogResponse> getCatalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) NovelStatus status,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(catalogService.getCatalog(q, language, status, sort, page, size));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}