package com.seaoftales.identity.dto;

import com.seaoftales.content.dto.PublicHomeResponse;

import java.util.List;

public record PublicAuthorResponse(
        String username,
        String profilePicture,
        long publicNovelCount,
        List<PublicHomeResponse.NovelCard> novels) {
}
