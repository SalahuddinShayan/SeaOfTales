package com.seaoftales.reaction.service;

import com.seaoftales.content.entity.Chapter;
import com.seaoftales.content.enums.ChapterState;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.reaction.dto.ReactionSummary;
import com.seaoftales.reaction.entity.Reaction;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.enums.ReactionType;
import com.seaoftales.reaction.repository.ReactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class ReactionService {
    private final ReactionRepository reactions;
    private final UserRepository users;
    private final NovelRepository novels;
    private final ChapterRepository chapters;

    public ReactionService(ReactionRepository reactions, UserRepository users,
                           NovelRepository novels, ChapterRepository chapters) {
        this.reactions = reactions;
        this.users = users;
        this.novels = novels;
        this.chapters = chapters;
    }

    @Transactional(readOnly = true)
    public ReactionSummary summary(ReactionTargetType targetType, Long targetId, String username) {
        requirePositiveId(targetId);
        requireTarget(targetType, targetId);
        Long userId = username == null ? null : users.findByUsername(username).map(User::getId).orElse(null);
        return buildSummary(targetType, targetId, userId);
    }

    @Transactional
    public ReactionSummary toggle(String username, ReactionTargetType targetType, Long targetId,
                                  ReactionType reactionType) {
        requirePositiveId(targetId);
        requireTarget(targetType, targetId);

        // Serializes writes for this user while allowing unrelated users/targets to proceed concurrently.
        User user = users.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found."));
        Reaction existing = reactions.findByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId)
                .orElse(null);
        if (existing == null) {
            reactions.save(new Reaction(user.getId(), targetType, targetId, reactionType));
        } else if (existing.getReactionType() == reactionType) {
            reactions.delete(existing);
        } else {
            existing.setReactionType(reactionType);
            reactions.save(existing);
        }
        return buildSummary(targetType, targetId, user.getId());
    }

    private void requireTarget(ReactionTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case NOVEL -> novels.existsById(targetId);
            case CHAPTER -> chapters.existsPublicChapterById(targetId);
        };
        if (!exists) throw new TargetNotFoundException();
    }

    private ReactionSummary buildSummary(ReactionTargetType targetType, Long targetId, Long userId) {
        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) counts.put(type, 0L);
        reactions.countByTarget(targetType, targetId)
                .forEach(row -> counts.put(row.getReactionType(), row.getReactionCount()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        ReactionType selected = userId == null ? null : reactions
                .findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .map(Reaction::getReactionType).orElse(null);
        return new ReactionSummary(counts, total, selected);
    }

    private void requirePositiveId(Long targetId) {
        if (targetId == null || targetId < 1) throw new IllegalArgumentException("Invalid reaction target ID.");
    }

    public static class TargetNotFoundException extends RuntimeException {}
}
