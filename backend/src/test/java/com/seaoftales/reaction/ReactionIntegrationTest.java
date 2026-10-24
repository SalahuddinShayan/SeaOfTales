package com.seaoftales.reaction;

import com.seaoftales.content.entity.Chapter;
import com.seaoftales.content.entity.ChapterEdition;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.ChapterState;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.repository.ChapterEditionRepository;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.enums.UserRole;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.reaction.entity.Reaction;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;
import com.seaoftales.reaction.repository.ReactionRepository;
import com.seaoftales.reaction.service.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReactionIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ReactionService service;
    @Autowired private ReactionRepository reactions;
    @Autowired private UserRepository users;
    @Autowired private NovelRepository novels;
    @Autowired private ChapterRepository chapters;
    @Autowired private ChapterEditionRepository editions;

    private String suffix;
    private User reader;
    private Novel novel;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        reader = saveUser("reader_" + suffix, UserRole.READER);
        saveUser("author_" + suffix, UserRole.AUTHOR);
        saveUser("editor_" + suffix, UserRole.EDITOR);
        saveUser("admin_" + suffix, UserRole.ADMIN);
        novel = new Novel();
        novel.setAuthorUsername("author_" + suffix);
        novel.setTitle("Reaction test novel");
        novel.setSourceLanguage(Language.JAPANESE);
        novel.setStatus(NovelStatus.ONGOING);
        novel = novels.save(novel);

        chapter = new Chapter();
        chapter.setNovelId(novel.getId());
        chapter.setChapterNumber("1");
        chapter.setState(ChapterState.EDITED);
        chapter = chapters.save(chapter);
        ChapterEdition edition = new ChapterEdition();
        edition.setChapterId(chapter.getId());
        edition.setTitle("Published");
        edition.setContent("<p>Public chapter</p>");
        editions.save(edition);
    }

    @Test
    void acceptsEachReactionTypeAndPersistsOneActiveReaction() {
        for (ReactionType type : ReactionType.values()) {
            var summary = service.toggle(reader.getUsername(), ReactionTargetType.NOVEL, novel.getId(), type);
            assertEquals(type, summary.currentUserReaction());
            assertEquals(1L, summary.total());
            assertEquals(1, reactions.count());
        }
        assertEquals(6, ReactionType.values().length);
        assertEquals(1, reactions.count());
    }

    @Test
    void changingAndRetogglingReplacesThenRemovesReaction() {
        service.toggle(reader.getUsername(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.LOVE);
        var changed = service.toggle(reader.getUsername(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.FIRE);
        assertEquals(ReactionType.FIRE, changed.currentUserReaction());
        assertEquals(1L, changed.counts().get(ReactionType.FIRE));
        assertEquals(0L, changed.counts().get(ReactionType.LOVE));

        var removed = service.toggle(reader.getUsername(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.FIRE);
        assertNull(removed.currentUserReaction());
        assertEquals(0, removed.total());
        assertTrue(java.util.Arrays.stream(ReactionType.values()).allMatch(type -> removed.counts().get(type) == 0));
    }

    @Test
    void novelAndChapterReactionsAndUsersRemainIndependent() {
        service.toggle(reader.getUsername(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.LOVE);
        service.toggle(reader.getUsername(), ReactionTargetType.CHAPTER, chapter.getId(), ReactionType.SAD);
        service.toggle("author_" + suffix, ReactionTargetType.NOVEL, novel.getId(), ReactionType.WOW);

        assertEquals(2L, service.summary(ReactionTargetType.NOVEL, novel.getId(), null).total());
        assertEquals(ReactionType.LOVE,
                service.summary(ReactionTargetType.NOVEL, novel.getId(), reader.getUsername()).currentUserReaction());
        assertEquals(1L, service.summary(ReactionTargetType.CHAPTER, chapter.getId(), null).total());
        assertEquals(ReactionType.SAD,
                service.summary(ReactionTargetType.CHAPTER, chapter.getId(), reader.getUsername()).currentUserReaction());
        assertNull(service.summary(ReactionTargetType.NOVEL, novel.getId(), "editor_" + suffix).currentUserReaction());
    }

    @Test
    void anonymousCanReadZeroFilledSummaryAndMutationRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/reactions/novel/{id}", novel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.counts.LOVE").value(0))
                .andExpect(jsonPath("$.counts.FIRE").value(0))
                .andExpect(jsonPath("$.currentUserReaction").doesNotExist());

        mvc.perform(put("/api/reactions/novel/{id}", novel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"LOVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyAuthenticatedRoleCanToggleAndReceiveUpdatedSummary() throws Exception {
        for (String role : new String[]{"READER", "AUTHOR", "EDITOR", "ADMIN"}) {
            String username = role.toLowerCase() + "_" + suffix;
            mvc.perform(put("/api/reactions/novel/{id}", novel.getId())
                            .with(user(username).roles(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reactionType\":\"LOVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentUserReaction").value("LOVE"));
        }
        assertEquals(4L, service.summary(ReactionTargetType.NOVEL, novel.getId(), null).total());
    }

    @Test
    void rejectsInvalidAndMissingTargets() throws Exception {
        mvc.perform(get("/api/reactions/invalid/{id}", novel.getId())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reactions/novel/999999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/reactions/chapter/{id}", chapter.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.SAD").value(0));
        mvc.perform(put("/api/reactions/novel/{id}", novel.getId())
                        .with(user(reader.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"NOT_A_REACTION\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/reactions/chapter/999999999")
                        .with(user(reader.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"LOVE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void databaseConstraintPreventsDuplicateActiveReactions() {
        reactions.saveAndFlush(new Reaction(reader.getId(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.LOVE));
        assertThrows(DataIntegrityViolationException.class, () -> reactions.saveAndFlush(
                new Reaction(reader.getId(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.FIRE)));
    }

    private User saveUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPasswordHash("test-hash");
        user.setRole(role);
        return users.save(user);
    }
}
