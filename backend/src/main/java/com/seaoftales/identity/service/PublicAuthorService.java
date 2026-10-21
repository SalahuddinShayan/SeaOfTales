package com.seaoftales.identity.service;

import com.seaoftales.content.dto.PublicHomeResponse;
import com.seaoftales.identity.dto.PublicAuthorResponse;
import com.seaoftales.identity.repository.PublicAuthorNovelRow;
import com.seaoftales.identity.repository.PublicAuthorRow;
import com.seaoftales.identity.repository.PublicAuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PublicAuthorService {
    private final PublicAuthorRepository authors;

    public PublicAuthorService(PublicAuthorRepository authors) {
        this.authors = authors;
    }

    public List<PublicAuthorResponse> getAuthors() {
        Map<String, List<PublicAuthorRow>> rowsByUsername = new LinkedHashMap<>();
        for (PublicAuthorRow row : authors.findPublicAuthors()) {
            rowsByUsername.computeIfAbsent(row.getUsername(), ignored -> new ArrayList<>()).add(row);
        }
        return rowsByUsername.values().stream().map(rows -> {
            PublicAuthorRow author = rows.getFirst();
            List<PublicHomeResponse.NovelCard> previews = rows.stream()
                    .filter(row -> row.getNovelId() != null)
                    .map(this::toNovelCard)
                    .toList();
            return new PublicAuthorResponse(
                    author.getUsername(), pictureUrl(author.getUsername(), author.getProfilePicture()), author.getPublicNovelCount(), previews);
        }).toList();
    }

    public PublicAuthorResponse getAuthor(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Author not found.");
        List<PublicAuthorNovelRow> rows = authors.findPublicAuthor(username.trim());
        if (rows.isEmpty()) throw new IllegalArgumentException("Author not found.");
        PublicAuthorNovelRow first = rows.getFirst();
        List<PublicHomeResponse.NovelCard> novels = rows.stream()
                .filter(row -> row.getNovelId() != null)
                .map(row -> new PublicHomeResponse.NovelCard(
                        row.getNovelId(), row.getTitle(), row.getStatus(), row.getSourceLanguage(),
                        row.getDescription(), row.getCoverImage()))
                .toList();
        return new PublicAuthorResponse(first.getUsername(), pictureUrl(first.getUsername(), first.getProfilePicture()), novels.size(), novels);
    }

    private PublicHomeResponse.NovelCard toNovelCard(PublicAuthorRow row) {
        return new PublicHomeResponse.NovelCard(
                row.getNovelId(), row.getTitle(), row.getStatus(), row.getSourceLanguage(),
                row.getDescription(), row.getCoverImage());
    }

    private String pictureUrl(String username, String reference) {
        return reference == null || reference.isBlank() ? null : "/api/profiles/" + username + "/picture";
    }
}
