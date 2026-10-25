package com.seaoftales.comment;

import com.seaoftales.comment.dto.CommentDtos;
import com.seaoftales.comment.entity.Comment;
import com.seaoftales.comment.entity.CommentReport;
import com.seaoftales.comment.entity.CommentVote;
import com.seaoftales.comment.enums.CommentReportCategory;
import com.seaoftales.comment.enums.CommentReportStatus;
import com.seaoftales.comment.enums.CommentTargetType;
import com.seaoftales.comment.enums.CommentVoteType;
import com.seaoftales.comment.repository.CommentReportRepository;
import com.seaoftales.comment.repository.CommentRepository;
import com.seaoftales.comment.repository.CommentVoteRepository;
import com.seaoftales.comment.service.CommentService;
import com.seaoftales.comment.service.CommentApiException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${seaoftales.comments.test-url:jdbc:h2:mem:comment_api_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
        "spring.datasource.driver-class-name=${seaoftales.comments.test-driver:org.h2.Driver}",
        "spring.jpa.hibernate.ddl-auto=${seaoftales.comments.test-ddl-auto:create-drop}"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private CommentService service;
    @Autowired private CommentRepository comments;
    @Autowired private CommentVoteRepository votes;
    @Autowired private CommentReportRepository reports;
    @Autowired private UserRepository users;
    @Autowired private NovelRepository novels;
    @Autowired private ChapterRepository chapters;
    @Autowired private ChapterEditionRepository editions;

    private String suffix;
    private User owner;
    private User other;
    private User admin;
    private Novel novel;
    private Chapter chapter;
    private Chapter hiddenChapter;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        owner = saveUser("owner_" + suffix, UserRole.AUTHOR);
        other = saveUser("other_" + suffix, UserRole.READER);
        admin = saveUser("admin_" + suffix, UserRole.ADMIN);
        saveUser("reader_" + suffix, UserRole.READER);
        saveUser("author_" + suffix, UserRole.AUTHOR);
        saveUser("editor_" + suffix, UserRole.EDITOR);
        novel = new Novel();
        novel.setAuthorUsername(owner.getUsername());
        novel.setTitle("Comment API novel");
        novel.setSourceLanguage(Language.JAPANESE);
        novel.setStatus(NovelStatus.ONGOING);
        novel = novels.saveAndFlush(novel);
        chapter = createChapter(ChapterState.EDITED, true);
        hiddenChapter = createChapter(ChapterState.UPLOADED, false);
    }

    @Test
    void guestCanReadPublicDiscussionButCannotWrite() throws Exception {
        Comment comment = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "<script>alert(1)</script>");
        mvc.perform(get("/api/comments/novel/{id}", novel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment.body").value("<script>alert(1)</script>"))
                .andExpect(jsonPath("$.content[0].comment.authorUsername").value(owner.getUsername()))
                .andExpect(jsonPath("$.content[0].comment.currentUserVote").doesNotExist())
                .andExpect(jsonPath("$.content[0].comment.email").doesNotExist())
                .andExpect(jsonPath("$.content[0].comment.passwordHash").doesNotExist());
        mvc.perform(post("/api/comments/novel/{id}", novel.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"guest\"}"))
                .andExpect(status().isForbidden());
        assertNotNull(comment.getId());
    }

    @Test
    void allAuthenticatedRolesCanCreateComments() throws Exception {
        for (String role : new String[]{"READER", "AUTHOR", "EDITOR", "ADMIN"}) {
            String username = role.toLowerCase() + "_" + suffix;
            mvc.perform(post("/api/comments/novel/{id}", novel.getId())
                            .with(user(username).roles(role)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"from " + role + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.authorUsername").value(username));
        }
    }

    @Test
    void validatesTargetsAndOnlyAllowsRepliesOnMatchingTopLevelParents() throws Exception {
        mvc.perform(get("/api/comments/other/{id}", novel.getId())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/comments/novel/999999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/comments/chapter/{id}", hiddenChapter.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/api/comments/chapter/{id}", hiddenChapter.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"blocked\"}"))
                .andExpect(status().isNotFound());

        Comment parent = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Parent body");
        mvc.perform(post("/api/comments/chapter/{target}/{parent}/replies", chapter.getId(), parent.getId())
                        .with(user(other.getUsername()).roles("READER")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"cross target\"}"))
                .andExpect(status().isBadRequest());

        Comment reply = createComment(other, CommentTargetType.NOVEL, novel.getId(), parent.getId(), "Reply body");
        mvc.perform(post("/api/comments/novel/{target}/{parent}/replies", novel.getId(), reply.getId())
                        .with(user(admin.getUsername()).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"nested\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsRepliesAndReturnsStableBoundedThreadData() throws Exception {
        Comment parent = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Parent body");
        mvc.perform(post("/api/comments/novel/{target}/{parent}/replies", novel.getId(), parent.getId())
                        .with(user(other.getUsername()).roles("READER")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"first reply\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/comments/novel/{id}?page=0&size=5", novel.getId()).with(user(owner.getUsername()).roles("AUTHOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment.id").value(parent.getId()))
                .andExpect(jsonPath("$.content[0].replyCount").value(1))
                .andExpect(jsonPath("$.content[0].replies[0].body").value("first reply"))
                .andExpect(jsonPath("$.content[0].replies[0].currentUserVote").doesNotExist());
        mvc.perform(get("/api/comments/novel/{target}/{parent}/replies?page=0&size=1", novel.getId(), parent.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].body").value("first reply"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/comments/chapter/{target}/{parent}/replies", chapter.getId(), parent.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/comments/novel/{id}?size=51", novel.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void createsEditsWithoutChangingTargetAndEnforcesOwnerOrAdmin() throws Exception {
        Comment comment = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Original");
        mvc.perform(patch("/api/comments/{id}", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"unauthorized\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/comments/{id}", comment.getId()).with(user(owner.getUsername()).roles("AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Edited\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("Edited"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());
        mvc.perform(patch("/api/comments/{id}", comment.getId()).with(user(admin.getUsername()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Admin edit\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("Admin edit"));
        assertEquals(novel.getId(), comments.findById(comment.getId()).orElseThrow().getTargetId());
    }

    @Test
    void rejectsBlankOversizedDuplicateContentAndCooldownAcrossTargetKinds() throws Exception {
        mvc.perform(post("/api/comments/novel/{id}", novel.getId()).with(user(owner.getUsername()).roles("AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/comments/novel/{id}", novel.getId()).with(user(owner.getUsername()).roles("AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + "x".repeat(3001) + "\"}"))
                .andExpect(status().isBadRequest());
        service.create(owner.getUsername(), CommentTargetType.NOVEL, novel.getId(), "Repeated text");
        CommentApiException duplicate = assertThrows(CommentApiException.class,
                () -> service.create(owner.getUsername(), CommentTargetType.NOVEL, novel.getId(), "Repeated text"));
        assertEquals(409, duplicate.getStatus().value());
        CommentApiException cooldown = assertThrows(CommentApiException.class,
                () -> service.create(owner.getUsername(), CommentTargetType.CHAPTER, chapter.getId(), "Different text"));
        assertEquals(429, cooldown.getStatus().value());
    }

    @Test
    void supportsVoteCreateReplaceToggleAndCurrentSelection() throws Exception {
        Comment comment = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Vote me");
        mvc.perform(put("/api/comments/{id}/vote", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"voteType\":\"LIKE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(1)).andExpect(jsonPath("$.currentUserVote").value("LIKE"));
        mvc.perform(put("/api/comments/{id}/vote", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"voteType\":\"DISLIKE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes").value(0)).andExpect(jsonPath("$.dislikes").value(1));
        mvc.perform(put("/api/comments/{id}/vote", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"voteType\":\"DISLIKE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dislikes").value(0)).andExpect(jsonPath("$.currentUserVote").doesNotExist());
        mvc.perform(put("/api/comments/999999999/vote").with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"voteType\":\"LIKE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateReportsAndAllowsAdminQueueResolution() throws Exception {
        Comment comment = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Report me");
        mvc.perform(post("/api/comments/{id}/reports", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"SPAM\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(post("/api/comments/{id}/reports", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"SPAM\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/comment-reports").with(user(other.getUsername()).roles("READER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/comment-reports")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/comment-reports").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].reporterUsername").value(other.getUsername()));
        CommentReport report = reports.findByUserIdAndCommentId(other.getId(), comment.getId()).orElseThrow();
        mvc.perform(patch("/api/admin/comment-reports/{id}", report.getId()).with(user(admin.getUsername()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISMISSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.resolverUsername").value(admin.getUsername()));
        mvc.perform(post("/api/comments/{id}/reports", comment.getId()).with(user(other.getUsername()).roles("READER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"OTHER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingParentCleansReplyVotesAndReportsAndAdminCanDeleteOthers() throws Exception {
        Comment parent = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Delete parent");
        Comment reply = createComment(other, CommentTargetType.NOVEL, novel.getId(), parent.getId(), "Delete reply");
        votes.saveAndFlush(new CommentVote(other.getId(), parent.getId(), CommentVoteType.LIKE));
        votes.saveAndFlush(new CommentVote(owner.getId(), reply.getId(), CommentVoteType.DISLIKE));
        reports.saveAndFlush(new CommentReport(other.getId(), parent.getId(), CommentReportCategory.SPAM));
        reports.saveAndFlush(new CommentReport(owner.getId(), reply.getId(), CommentReportCategory.ABUSE));

        mvc.perform(delete("/api/comments/{id}", parent.getId()).with(user(other.getUsername()).roles("READER")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/comments/{id}", parent.getId()).with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertFalse(comments.existsById(parent.getId()));
        assertFalse(comments.existsById(reply.getId()));
        assertEquals(0, votes.count());
        assertEquals(0, reports.count());

        Comment another = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Admin can delete");
        mvc.perform(delete("/api/comments/{id}", another.getId()).with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingReplyRemovesItsVotesAndReports() {
        Comment parent = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Parent");
        Comment reply = createComment(other, CommentTargetType.NOVEL, novel.getId(), parent.getId(), "Reply");
        votes.saveAndFlush(new CommentVote(owner.getId(), reply.getId(), CommentVoteType.LIKE));
        reports.saveAndFlush(new CommentReport(owner.getId(), reply.getId(), CommentReportCategory.OTHER));

        service.delete(other.getUsername(), reply.getId());

        assertTrue(comments.existsById(parent.getId()));
        assertFalse(comments.existsById(reply.getId()));
        assertEquals(0, votes.count());
        assertEquals(0, reports.count());
    }

    @Test
    void anonymousVoteAndReportWritesAreForbidden() throws Exception {
        Comment comment = createComment(owner, CommentTargetType.NOVEL, novel.getId(), null, "Private actions");
        mvc.perform(put("/api/comments/{id}/vote", comment.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"voteType\":\"LIKE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/comments/{id}/reports", comment.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"SPAM\"}"))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPasswordHash("test-hash");
        user.setRole(role);
        user.setProfilePicture("images/" + username + ".png");
        return users.saveAndFlush(user);
    }

    private Chapter createChapter(ChapterState state, boolean withEdition) {
        Chapter item = new Chapter();
        item.setNovelId(novel.getId());
        item.setChapterNumber(UUID.randomUUID().toString());
        item.setState(state);
        item = chapters.saveAndFlush(item);
        if (withEdition) {
            ChapterEdition edition = new ChapterEdition();
            edition.setChapterId(item.getId());
            edition.setTitle("Public chapter");
            edition.setContent("<p>Reader visible</p>");
            editions.saveAndFlush(edition);
        }
        return item;
    }

    private Comment createComment(User author, CommentTargetType targetType, Long targetId, Long parentId, String body) {
        return comments.saveAndFlush(new Comment(author.getId(), targetType, targetId, parentId, body));
    }
}
