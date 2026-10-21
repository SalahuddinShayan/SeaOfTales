package com.seaoftales.identity;

import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.enums.UserRole;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.identity.service.PublicAuthorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PublicAuthorQueryIntegrationTest {
    @Autowired UserRepository users;
    @Autowired NovelRepository novels;
    @Autowired PublicAuthorService authors;

    @Test
    void sqlIncludesEmptyAuthorsExcludesOtherRolesAndReturnsOnlyPublicProfileFields() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User author = user("author_" + suffix, UserRole.AUTHOR);
        author.setProfilePicture("/api/profile-pictures/author.png");
        users.save(author);
        User emptyAuthor = user("empty_" + suffix, UserRole.AUTHOR);
        user("reader_" + suffix, UserRole.READER);
        novel(author.getUsername(), "Ongoing", NovelStatus.ONGOING);
        novel(author.getUsername(), "Upcoming", NovelStatus.UPCOMING);
        novel(author.getUsername(), "Completed", NovelStatus.COMPLETED);
        novel(author.getUsername(), "Hiatus", NovelStatus.HIATUS);
        novel(author.getUsername(), "Dropped", NovelStatus.DROPPED);

        var directory = authors.getAuthors();
        var listed = directory.stream().filter(item -> item.username().equals(author.getUsername())).findFirst().orElseThrow();
        var empty = directory.stream().filter(item -> item.username().equals(emptyAuthor.getUsername())).findFirst().orElseThrow();

        assertEquals(5, listed.publicNovelCount());
        assertEquals(3, listed.novels().size());
        assertEquals("/api/profiles/" + author.getUsername() + "/picture", listed.profilePicture());
        assertEquals(0, empty.publicNovelCount());
        assertFalse(directory.stream().anyMatch(item -> item.username().startsWith("reader_" + suffix)));
        var profile = authors.getAuthor(author.getUsername());
        assertEquals(5, profile.novels().size());
        assertTrue(profile.novels().stream().anyMatch(item -> item.status().equals("UPCOMING")));
        var responseFields = profile.getClass().getRecordComponents();
        assertNotNull(responseFields);
        assertFalse(java.util.Arrays.stream(responseFields).anyMatch(field ->
                field.getName().toLowerCase().contains("email") || field.getName().toLowerCase().contains("password")));
    }

    private User user(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPasswordHash("private-test-hash");
        user.setRole(role);
        return users.save(user);
    }

    private void novel(String authorUsername, String title, NovelStatus status) {
        Novel novel = new Novel();
        novel.setAuthorUsername(authorUsername);
        novel.setTitle(title);
        novel.setSourceLanguage(Language.JAPANESE);
        novel.setStatus(status);
        novels.save(novel);
    }
}
