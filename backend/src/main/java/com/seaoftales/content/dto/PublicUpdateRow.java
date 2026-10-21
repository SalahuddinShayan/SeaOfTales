package com.seaoftales.content.dto;

import java.time.Instant;

public record PublicUpdateRow(Long chapterId, Long novelId, String novelTitle, String coverImage,
                             String chapterNumber, String chapterTitle, Instant updatedAt) { }
