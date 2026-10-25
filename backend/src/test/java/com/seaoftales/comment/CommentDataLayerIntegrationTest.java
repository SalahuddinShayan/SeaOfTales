package com.seaoftales.comment;

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
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.enums.UserRole;
import com.seaoftales.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${seaoftales.comments.test-url:jdbc:h2:mem:comment_data_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
        "spring.datasource.driver-class-name=${seaoftales.comments.test-driver:org.h2.Driver}",
        "spring.jpa.hibernate.ddl-auto=${seaoftales.comments.test-ddl-auto:create-drop}"
})
@ActiveProfiles("test")
@Transactional
class CommentDataLayerIntegrationTest {
    @Autowired private CommentRepository comments;
    @Autowired private CommentVoteRepository votes;
    @Autowired private CommentReportRepository reports;
    @Autowired private UserRepository users;

    private Long authorId;
    private Long secondUserId;

    @BeforeEach
    void setUp() {
        authorId = createUser("comment_author").getId();
        secondUserId = createUser("comment_voter").getId();
    }

    @Test
    void persistsCommentsAndKeepsNovelAndChapterTargetsIndependentForSameNumericId() {
        Comment novelComment = comments.saveAndFlush(new Comment(authorId, CommentTargetType.NOVEL, 700L, null, "Novel discussion"));
        Comment chapterComment = comments.saveAndFlush(new Comment(authorId, CommentTargetType.CHAPTER, 700L, null, "Chapter discussion"));

        assertNotEquals(novelComment.getId(), chapterComment.getId());
        assertEquals(List.of(novelComment), comments.findTargetComments(CommentTargetType.NOVEL, 700L, PageRequest.of(0, 20)));
        assertEquals(List.of(chapterComment), comments.findTargetComments(CommentTargetType.CHAPTER, 700L, PageRequest.of(0, 20)));
    }

    @Test
    void retrievesTopLevelCommentsAndRepliesInStableChronologicalOrder() {
        Comment parent = comments.saveAndFlush(new Comment(authorId, CommentTargetType.NOVEL, 701L, null, "Parent"));
        Comment first = comments.saveAndFlush(new Comment(secondUserId, CommentTargetType.NOVEL, 701L, parent.getId(), "First reply"));
        Comment second = comments.saveAndFlush(new Comment(authorId, CommentTargetType.NOVEL, 701L, parent.getId(), "Second reply"));
        Comment secondParent = comments.saveAndFlush(new Comment(secondUserId, CommentTargetType.NOVEL, 701L, null, "Second top-level"));

        assertEquals(List.of(parent), comments.findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtAscIdAsc(
                CommentTargetType.NOVEL, 701L, PageRequest.of(0, 1)).getContent());
        assertEquals(List.of(parent, secondParent), comments.findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtAscIdAsc(
                CommentTargetType.NOVEL, 701L, PageRequest.of(0, 20)).getContent());
        List<Comment> replies = comments.findByParentCommentIdOrderByCreatedAtAscIdAsc(parent.getId(), PageRequest.of(0, 20)).getContent();
        assertEquals(List.of(first, second), replies);
        assertEquals(List.of(parent, first, second), comments.findTargetComments(
                CommentTargetType.NOVEL, 701L, PageRequest.of(0, 3)));
    }

    @Test
    void voteConstraintLookupAndAggregatesIncludeCommentsWithNoVotes() {
        Comment withVotes = comments.saveAndFlush(new Comment(authorId, CommentTargetType.NOVEL, 702L, null, "Voted"));
        Comment withoutVotes = comments.saveAndFlush(new Comment(authorId, CommentTargetType.NOVEL, 702L, null, "Unvoted"));
        votes.saveAndFlush(new CommentVote(authorId, withVotes.getId(), CommentVoteType.LIKE));
        votes.saveAndFlush(new CommentVote(secondUserId, withVotes.getId(), CommentVoteType.DISLIKE));

        assertEquals(CommentVoteType.LIKE, votes.findByUserIdAndCommentId(authorId, withVotes.getId()).orElseThrow().getVoteType());
        assertEquals(2L, votes.countByCommentId(withVotes.getId()).stream().mapToLong(CommentVoteRepository.VoteCount::getVoteCount).sum());
        assertTrue(votes.countByCommentId(withoutVotes.getId()).isEmpty());
        assertThrows(DataIntegrityViolationException.class, () -> votes.saveAndFlush(
                new CommentVote(authorId, withVotes.getId(), CommentVoteType.DISLIKE)));
    }

    @Test
    void reportConstraintLookupAndStatusQueueOrderingArePersisted() {
        Comment comment = comments.saveAndFlush(new Comment(authorId, CommentTargetType.CHAPTER, 703L, null, "Reported"));
        Comment secondComment = comments.saveAndFlush(new Comment(secondUserId, CommentTargetType.CHAPTER, 703L, null, "Also reported"));
        CommentReport first = reports.saveAndFlush(new CommentReport(authorId, comment.getId(), CommentReportCategory.SPAM));
        CommentReport second = reports.saveAndFlush(new CommentReport(secondUserId, comment.getId(), CommentReportCategory.OTHER));
        second.setStatus(CommentReportStatus.DISMISSED);
        reports.saveAndFlush(second);
        CommentReport third = reports.saveAndFlush(new CommentReport(authorId, secondComment.getId(), CommentReportCategory.ABUSE));

        assertEquals(first.getId(), reports.findByUserIdAndCommentId(authorId, comment.getId()).orElseThrow().getId());
        assertEquals(List.of(first, third), reports.findByStatusOrderByCreatedAtAscIdAsc(CommentReportStatus.PENDING, PageRequest.of(0, 20)).getContent());
        assertEquals(List.of(second), reports.findByStatusOrderByCreatedAtAscIdAsc(CommentReportStatus.DISMISSED, PageRequest.of(0, 20)).getContent());
        assertThrows(DataIntegrityViolationException.class, () -> reports.saveAndFlush(
                new CommentReport(authorId, comment.getId(), CommentReportCategory.HARASSMENT)));
    }

    @Test
    void databaseColumnAcceptsMaximumBodyAndRejectsLongerText() {
        Comment maximum = comments.saveAndFlush(new Comment(authorId, CommentTargetType.NOVEL, 704L, null, "x".repeat(3000)));
        assertEquals(3000, maximum.getBody().length());
        assertThrows(DataIntegrityViolationException.class, () -> comments.saveAndFlush(
                new Comment(authorId, CommentTargetType.NOVEL, 704L, null, "x".repeat(3001))));
    }

    private User createUser(String prefix) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPasswordHash("test-hash");
        user.setRole(UserRole.READER);
        return users.saveAndFlush(user);
    }
}
