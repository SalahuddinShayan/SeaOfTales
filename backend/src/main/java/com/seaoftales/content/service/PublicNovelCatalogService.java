package com.seaoftales.content.service;

import com.seaoftales.content.dto.NovelCatalogResponse;
import com.seaoftales.content.dto.PublicHomeResponse;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.repository.NovelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicNovelCatalogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NovelRepository novelRepository;

    public PublicNovelCatalogService(NovelRepository novelRepository) {
        this.novelRepository = novelRepository;
    }

    public NovelCatalogResponse getCatalog(
            String query,
            Language language,
            NovelStatus status,
            String sort,
            int page,
            int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        String titleQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageRequest = PageRequest.of(page, size, standardSort(sort));
        if ("updated".equals(sort)) {
            Page<Long> novelIds = novelRepository.findRecentlyUpdatedNovelIds(
                    titleQuery, language, status, pageRequest);
            Map<Long, Novel> novelsById = novelRepository.findAllById(novelIds.getContent()).stream()
                    .collect(Collectors.toMap(Novel::getId, Function.identity()));
            return new NovelCatalogResponse(
                    novelIds.getContent().stream()
                            .map(novelsById::get)
                            .filter(novel -> novel != null)
                            .map(this::toNovelCard)
                            .toList(),
                    page,
                    size,
                    novelIds.getTotalElements(),
                    novelIds.getTotalPages());
        }

        Page<Novel> novels = novelRepository.findAll(filterBy(titleQuery, language, status), pageRequest);
        return new NovelCatalogResponse(
                novels.getContent().stream().map(this::toNovelCard).toList(),
                page,
                size,
                novels.getTotalElements(),
                novels.getTotalPages());
    }

    private Sort standardSort(String sort) {
        return switch (sort) {
            case "updated" -> Sort.unsorted();
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case "title" -> Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id"));
            default -> throw new IllegalArgumentException("Unsupported catalog sort option.");
        };
    }

    private Specification<Novel> filterBy(String query, Language language, NovelStatus status) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            if (query != null) {
                String escapedQuery = query.toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
                                "%" + escapedQuery + "%", '\\'));
            }
            if (language != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("sourceLanguage"), language));
            }
            if (status != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("status"), status));
            }
            return predicates;
        };
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
}