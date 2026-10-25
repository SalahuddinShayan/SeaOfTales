package com.seaoftales.identity.dto;

import java.util.List;

public record ProfileActivityPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }
