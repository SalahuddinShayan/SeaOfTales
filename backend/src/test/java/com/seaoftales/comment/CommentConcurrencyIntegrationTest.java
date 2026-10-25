package com.seaoftales.comment;

import com.seaoftales.comment.entity.Comment;
import com.seaoftales.comment.enums.CommentReportCategory;
import com.seaoftales.comment.enums.CommentTargetType;
import com.seaoftales.comment.enums.CommentVoteType;
import com.seaoftales.comment.repository.CommentReportRepository;
import com.seaoftales.comment.repository.CommentRepository;
import com.seaoftales.comment.repository.CommentVoteRepository;
import com.seaoftales.comment.service.CommentApiException;
import com.seaoftales.comment.service.CommentService;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.enums.UserRole;
import com.seaoftales.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${seaoftales.comments.test-url:jdbc:h2:mem:comment_concurrency_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
        "spring.datasource.driver-class-name=${seaoftales.comments.test-driver:org.h2.Driver}",
        "spring.jpa.hibernate.ddl-auto=${seaoftales.comments.test-ddl-auto:create-drop}"
})
@ActiveProfiles("test")
class CommentConcurrencyIntegrationTest {
    @Autowired private TransactionTemplate transactions;
    @Autowired private UserRepository users;
    @Autowired private NovelRepository novels;
    @Autowired private CommentRepository comments;
    @Autowired private CommentVoteRepository votes;
    @Autowired private CommentReportRepository reports;
    @Autowired private CommentService service;

    @Test
    void concurrentVoteChangesLeaveOneActiveVote() throws Exception {
        Fixture fixture = createFixture();
        try {
            var pool = Executors.newFixedThreadPool(2);
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            try {
                var like = pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return service.toggleVote(fixture.username(), fixture.commentId(), CommentVoteType.LIKE);
                });
                var dislike = pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return service.toggleVote(fixture.username(), fixture.commentId(), CommentVoteType.DISLIKE);
                });
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                like.get(10, TimeUnit.SECONDS);
                dislike.get(10, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }

            assertEquals(1, votes.countByCommentId(fixture.commentId()).stream()
                    .mapToLong(CommentVoteRepository.VoteCount::getVoteCount).sum());
            assertNotNull(votes.findByUserIdAndCommentId(fixture.userId(), fixture.commentId()).orElseThrow());
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void concurrentDuplicateReportsCreateOnlyOneRecord() throws Exception {
        Fixture fixture = createFixture();
        try {
            var pool = Executors.newFixedThreadPool(2);
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            try {
                var first = pool.submit(() -> submitReport(fixture, CommentReportCategory.SPAM, ready, start));
                var second = pool.submit(() -> submitReport(fixture, CommentReportCategory.ABUSE, ready, start));
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                boolean firstCreated = first.get(10, TimeUnit.SECONDS);
                boolean secondCreated = second.get(10, TimeUnit.SECONDS);
                assertNotEquals(firstCreated, secondCreated);
            } finally {
                pool.shutdownNow();
            }
            assertEquals(1, reports.countByCommentId(fixture.commentId()));
            assertTrue(reports.findByUserIdAndCommentId(fixture.userId(), fixture.commentId()).isPresent());
        } finally {
            cleanup(fixture);
        }
    }

    private boolean submitReport(Fixture fixture, CommentReportCategory category,
                                 CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            service.report(fixture.username(), fixture.commentId(), category);
            return true;
        } catch (CommentApiException exception) {
            assertEquals(409, exception.getStatus().value());
            return false;
        }
    }

    private Fixture createFixture() {
        String username = "cc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        return transactions.execute(status -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(username + "@example.test");
            user.setPasswordHash("test-hash");
            user.setRole(UserRole.READER);
            user = users.save(user);
            Novel novel = new Novel();
            novel.setAuthorUsername(username);
            novel.setTitle("Comment concurrency target");
            novel.setSourceLanguage(Language.JAPANESE);
            novel.setStatus(NovelStatus.ONGOING);
            novel = novels.save(novel);
            Comment comment = comments.save(new Comment(user.getId(), CommentTargetType.NOVEL, novel.getId(), null, "Test comment"));
            return new Fixture(username, user.getId(), novel.getId(), comment.getId());
        });
    }

    private void cleanup(Fixture fixture) {
        transactions.executeWithoutResult(status -> {
            reports.deleteByCommentId(fixture.commentId());
            votes.deleteByCommentId(fixture.commentId());
            comments.deleteById(fixture.commentId());
            novels.deleteById(fixture.novelId());
            users.deleteById(fixture.userId());
        });
    }

    private record Fixture(String username, Long userId, Long novelId, Long commentId) { }
}
