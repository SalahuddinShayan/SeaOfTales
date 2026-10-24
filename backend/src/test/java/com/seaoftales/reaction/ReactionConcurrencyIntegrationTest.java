package com.seaoftales.reaction;

import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.enums.UserRole;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;
import com.seaoftales.reaction.repository.ReactionRepository;
import com.seaoftales.reaction.service.ReactionService;
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

@SpringBootTest
@ActiveProfiles("test")
class ReactionConcurrencyIntegrationTest {
    @Autowired private TransactionTemplate transactions;
    @Autowired private UserRepository users;
    @Autowired private NovelRepository novels;
    @Autowired private ReactionRepository reactions;
    @Autowired private ReactionService service;

    @Test
    void concurrentChangesForOneUserLeaveOnlyOneActiveReaction() throws Exception {
        String username = "concurrent_" + UUID.randomUUID().toString().replace("-", "");
        Long novelId = transactions.execute(status -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(username + "@example.test");
            user.setPasswordHash("test-hash");
            user.setRole(UserRole.READER);
            users.save(user);
            Novel novel = new Novel();
            novel.setAuthorUsername(username);
            novel.setTitle("Concurrent reaction target");
            novel.setSourceLanguage(Language.JAPANESE);
            novel.setStatus(NovelStatus.ONGOING);
            return novels.save(novel).getId();
        });
        assertNotNull(novelId);

        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var love = pool.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.toggle(username, ReactionTargetType.NOVEL, novelId, ReactionType.LOVE);
            });
            var fire = pool.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.toggle(username, ReactionTargetType.NOVEL, novelId, ReactionType.FIRE);
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            love.get(10, TimeUnit.SECONDS);
            fire.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        var summary = service.summary(ReactionTargetType.NOVEL, novelId, username);
        assertEquals(1L, summary.total());
        assertTrue(summary.currentUserReaction() == ReactionType.LOVE || summary.currentUserReaction() == ReactionType.FIRE);
        assertEquals(1, reactions.countByTarget(ReactionTargetType.NOVEL, novelId).stream()
                .mapToLong(ReactionRepository.ReactionCount::getReactionCount).sum());
    }
}
