package com.seaoftales.identity;

import com.seaoftales.comment.entity.Comment;
import com.seaoftales.comment.entity.CommentReport;
import com.seaoftales.comment.enums.CommentReportCategory;
import com.seaoftales.comment.enums.CommentTargetType;
import com.seaoftales.comment.repository.CommentReportRepository;
import com.seaoftales.comment.repository.CommentRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicProfileActivityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private NovelRepository novels;
    @Autowired private ChapterRepository chapters;
    @Autowired private ChapterEditionRepository editions;
    @Autowired private CommentRepository comments;
    @Autowired private CommentReportRepository reports;
    @Autowired private ReactionRepository reactions;

    private String username;
    private User owner;
    private User replyAuthor;
    private Novel novel;
    private Chapter chapter;
    private Chapter hiddenChapter;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        username = "profile_activity_" + suffix;
        owner = saveUser(username, UserRole.READER);
        replyAuthor = saveUser("reply_author_" + suffix, UserRole.EDITOR);
        novel = saveNovel("Activity target");
        chapter = saveChapter(novel.getId(), ChapterState.EDITED, true);
        hiddenChapter = saveChapter(novel.getId(), ChapterState.UPLOADED, false);
    }

    @Test
    void publicCommentsIncludeOwnerRepliesAndSafeNovelAndChapterContext() throws Exception {
        Comment top = comments.saveAndFlush(new Comment(owner.getId(), CommentTargetType.CHAPTER, chapter.getId(), null,
                "<script>alert(1)</script>\nplain text"));
        Comment reply = comments.saveAndFlush(new Comment(owner.getId(), CommentTargetType.CHAPTER, chapter.getId(), top.getId(), "My reply"));
        comments.saveAndFlush(new Comment(owner.getId(), CommentTargetType.NOVEL, novel.getId(), null, "Novel discussion"));
        comments.saveAndFlush(new Comment(owner.getId(), CommentTargetType.CHAPTER, hiddenChapter.getId(), null, "not public"));
        comments.saveAndFlush(new Comment(replyAuthor.getId(), CommentTargetType.NOVEL, novel.getId(), null, "other user"));
        reports.saveAndFlush(new CommentReport(replyAuthor.getId(), top.getId(), CommentReportCategory.SPAM));

        mvc.perform(get("/api/profiles/{username}/activity/comments", username).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].body").value("Novel discussion"))
                .andExpect(jsonPath("$.content[0].targetType").value("NOVEL"))
                .andExpect(jsonPath("$.content[0].novelTitle").value("Activity target"))
                .andExpect(jsonPath("$.content[1].body").value("My reply"))
                .andExpect(jsonPath("$.content[1].targetType").value("CHAPTER"))
                .andExpect(jsonPath("$.content[1].novelId").value(novel.getId()))
                .andExpect(jsonPath("$.content[1].chapterTitle").value("Public chapter"))
                .andExpect(jsonPath("$.content[1].parentCommentId").value(top.getId()))
                .andExpect(jsonPath("$.content[1].parentAuthorUsername").value(username))
                .andExpect(jsonPath("$.content[2].body").value("<script>alert(1)</script>\nplain text"))
                .andExpect(jsonPath("$.content[2].reporterUsername").doesNotExist())
                .andExpect(jsonPath("$.content[2].email").doesNotExist())
                .andExpect(jsonPath("$.content[2].passwordHash").doesNotExist());
    }

    @Test
    void publicReactionsShowOnlyCurrentStoredReactionsWithNumericTargetContext() throws Exception {
        reactions.saveAndFlush(new Reaction(owner.getId(), ReactionTargetType.NOVEL, novel.getId(), ReactionType.LOVE));
        reactions.saveAndFlush(new Reaction(owner.getId(), ReactionTargetType.CHAPTER, chapter.getId(), ReactionType.FIRE));
        reactions.saveAndFlush(new Reaction(owner.getId(), ReactionTargetType.CHAPTER, hiddenChapter.getId(), ReactionType.SAD));

        mvc.perform(get("/api/profiles/{username}/activity/reactions", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].reactionType").value("FIRE"))
                .andExpect(jsonPath("$.content[0].targetType").value("CHAPTER"))
                .andExpect(jsonPath("$.content[0].targetId").value(chapter.getId()))
                .andExpect(jsonPath("$.content[0].novelId").value(novel.getId()))
                .andExpect(jsonPath("$.content[0].chapterTitle").value("Public chapter"))
                .andExpect(jsonPath("$.content[1].reactionType").value("LOVE"))
                .andExpect(jsonPath("$.content[1].novelTitle").value("Activity target"))
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist());
    }

    @Test
    void activityIsPagedAndOnlyExistsForPublicNonAuthorProfiles() throws Exception {
        comments.saveAndFlush(new Comment(owner.getId(), CommentTargetType.NOVEL, novel.getId(), null, "first"));
        comments.saveAndFlush(new Comment(owner.getId(), CommentTargetType.NOVEL, novel.getId(), null, "second"));
        mvc.perform(get("/api/profiles/{username}/activity/comments", username).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/profiles/missing_user/activity/comments")).andExpect(status().isNotFound());
        User author = saveUser("profile_author_" + UUID.randomUUID().toString().replace("-", ""), UserRole.AUTHOR);
        mvc.perform(get("/api/profiles/{username}/activity/reactions", author.getUsername())).andExpect(status().isNotFound());
        mvc.perform(get("/api/profiles/{username}/activity/comments", username).param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    private User saveUser(String name, UserRole role) {
        User user = new User(); user.setUsername(name); user.setEmail(name + "@example.test");
        user.setPasswordHash("test-hash"); user.setRole(role); return users.saveAndFlush(user);
    }

    private Novel saveNovel(String title) {
        Novel item = new Novel(); item.setAuthorUsername(username); item.setTitle(title);
        item.setSourceLanguage(Language.JAPANESE); item.setStatus(NovelStatus.ONGOING); return novels.saveAndFlush(item);
    }

    private Chapter saveChapter(Long novelId, ChapterState state, boolean isPublic) {
        Chapter item = new Chapter(); item.setNovelId(novelId); item.setChapterNumber(UUID.randomUUID().toString());
        item.setState(state); item = chapters.saveAndFlush(item);
        if (isPublic) {
            ChapterEdition edition = new ChapterEdition(); edition.setChapterId(item.getId());
            edition.setTitle("Public chapter"); edition.setContent("<p>Public reader content</p>"); editions.saveAndFlush(edition);
        }
        return item;
    }
}
