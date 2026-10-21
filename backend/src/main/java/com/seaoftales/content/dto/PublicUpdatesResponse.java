package com.seaoftales.content.dto;

import java.util.List;

public record PublicUpdatesResponse(List<PublicHomeResponse.NovelUpdate> content, int page, int size,
                                   long totalElements, int totalPages) { }
