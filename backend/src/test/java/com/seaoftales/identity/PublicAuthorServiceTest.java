package com.seaoftales.identity;

import com.seaoftales.identity.repository.PublicAuthorNovelRow;
import com.seaoftales.identity.repository.PublicAuthorRepository;
import com.seaoftales.identity.repository.PublicAuthorRow;
import com.seaoftales.identity.service.PublicAuthorService;
import org.springframework.data.jpa.repository.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicAuthorServiceTest {
    private final PublicAuthorRepository repository = mock(PublicAuthorRepository.class);
    private final PublicAuthorService service = new PublicAuthorService(repository);

    @Test
    void directoryIncludesAuthorRowsWithAndWithoutNovels() {
        when(repository.findPublicAuthors()).thenReturn(List.of(
                authorRow("writer_with_books", 2L), authorRow("new_writer", 0L)));

        var authors = service.getAuthors();

        assertEquals(List.of("writer_with_books", "new_writer"), authors.stream().map(a -> a.username()).toList());
        assertEquals(List.of(2L, 0L), authors.stream().map(a -> a.publicNovelCount()).toList());
        verify(repository).findPublicAuthors();
    }

    @Test
    void profileReturnsNovelsFromOwnershipJoinAndUsesExistingPublicStatuses() {
        when(repository.findPublicAuthor("writer_1")).thenReturn(List.of(
                novelRow("writer_1", 8L, "ONGOING"), novelRow("writer_1", 9L, "UPCOMING")));

        var profile = service.getAuthor("writer_1");

        assertEquals("writer_1", profile.username());
        assertEquals(2, profile.publicNovelCount());
        assertEquals(List.of(8L, 9L), profile.novels().stream().map(novel -> novel.id()).toList());
        assertTrue(profile.novels().stream().anyMatch(novel -> "UPCOMING".equals(novel.status())));
    }

    @Test
    void authorWithoutNovelsStillHasProfileAndMissingOrNonAuthorIsNotFound() {
        when(repository.findPublicAuthor("empty_author")).thenReturn(List.of(novelRow("empty_author", null, null)));
        when(repository.findPublicAuthor("reader")).thenReturn(List.of());

        var profile = service.getAuthor("empty_author");

        assertEquals(0, profile.publicNovelCount());
        assertTrue(profile.novels().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.getAuthor("reader"));
        assertThrows(IllegalArgumentException.class, () -> service.getAuthor("missing"));
    }

    @Test
    void publicResponseTypesContainNoEmailOrAuthenticationFields() {
        var authorFields = com.seaoftales.identity.dto.PublicAuthorResponse.class.getRecordComponents();
        assertNotNull(authorFields);
        assertFalse(List.of(authorFields).stream().anyMatch(field ->
                field.getName().toLowerCase().contains("email") || field.getName().toLowerCase().contains("password")
                        || field.getName().toLowerCase().contains("enabled") || field.getName().toLowerCase().contains("role")));
        assertFalse(List.of(com.seaoftales.content.dto.PublicHomeResponse.NovelCard.class.getRecordComponents())
                .stream().anyMatch(field -> field.getName().toLowerCase().contains("author")));
    }

    @Test
    void repositoryQueriesRestrictToAuthorsAndJoinByTheExistingUsernameKey() throws Exception {
        String directoryQuery = PublicAuthorRepository.class.getMethod("findPublicAuthors")
                .getAnnotation(Query.class).value().replaceAll("\\s+", " ");
        String profileQuery = PublicAuthorRepository.class.getMethod("findPublicAuthor", String.class)
                .getAnnotation(Query.class).value().replaceAll("\\s+", " ");

        assertTrue(directoryQuery.contains("LEFT JOIN ranked_public_novels n"));
        assertTrue(directoryQuery.contains("WHERE u.role = 'AUTHOR'"));
        assertTrue(directoryQuery.contains("COUNT(*) OVER (PARTITION BY n.author_username)"));
        assertTrue(directoryQuery.contains("n.author_novel_rank <= 3"));
        assertTrue(profileQuery.contains("LEFT JOIN novels n ON n.author_username = u.username"));
        assertTrue(profileQuery.contains("WHERE u.role = 'AUTHOR'"));
        assertFalse(profileQuery.contains("n.status ="));
    }

    private PublicAuthorRow authorRow(String username, Long count) {
        return new PublicAuthorRow() {
            public String getUsername() { return username; }
            public String getProfilePicture() { return null; }
            public Long getPublicNovelCount() { return count; }
            public Long getNovelId() { return null; }
            public String getTitle() { return null; }
            public String getStatus() { return null; }
            public String getSourceLanguage() { return null; }
            public String getDescription() { return null; }
            public String getCoverImage() { return null; }
        };
    }

    private PublicAuthorNovelRow novelRow(String username, Long id, String status) {
        return new PublicAuthorNovelRow() {
            public String getUsername() { return username; }
            public String getProfilePicture() { return null; }
            public Long getNovelId() { return id; }
            public String getTitle() { return id == null ? null : "Novel " + id; }
            public String getStatus() { return status; }
            public String getSourceLanguage() { return id == null ? null : "JAPANESE"; }
            public String getDescription() { return null; }
            public String getCoverImage() { return null; }
        };
    }
}
