package com.seaoftales.content.dto;

import java.util.List;

public record NovelCatalogResponse(
        List<PublicHomeResponse.NovelCard> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}