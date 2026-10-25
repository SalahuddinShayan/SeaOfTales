package com.seaoftales.identity.service;

import com.seaoftales.comment.entity.Comment;
import com.seaoftales.comment.enums.CommentTargetType;
import com.seaoftales.comment.repository.CommentRepository;
import com.seaoftales.content.dto.PublicActivityChapterRow;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.dto.ProfileActivityComment;
import com.seaoftales.identity.dto.ProfileActivityPage;
import com.seaoftales.identity.dto.ProfileActivityReaction;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.reaction.entity.Reaction;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.repository.ReactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicProfileActivityService {
    private final PublicProfileService profiles;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final UserRepository users;
    private final NovelRepository novels;
    private final ChapterRepository chapters;

    public PublicProfileActivityService(PublicProfileService profiles, CommentRepository comments,
                                        ReactionRepository reactions, UserRepository users,
                                        NovelRepository novels, ChapterRepository chapters) {
        this.profiles = profiles;
        this.comments = comments;
        this.reactions = reactions;
        this.users = users;
        this.novels = novels;
        this.chapters = chapters;
    }

    @Transactional(readOnly = true)
    public ProfileActivityPage<ProfileActivityComment> comments(String username, int page, int size) {
        User profileOwner = profiles.requirePublicProfileOwner(username);
        Page<Comment> result = comments.findPublicActivityByAuthorUserId(profileOwner.getId(), pageRequest(page, size));
        Map<Long, Comment> parents = findParents(result.getContent());
        Map<Long, User> parentAuthors = findParentAuthors(parents.values());
        ActivityTargets targets = findTargets(result.getContent(), List.of());
        List<ProfileActivityComment> content = result.getContent().stream().map(comment -> {
            Comment parent = comment.getParentCommentId() == null ? null : parents.get(comment.getParentCommentId());
            User parentAuthor = parent == null ? null : parentAuthors.get(parent.getAuthorUserId());
            return new ProfileActivityComment(comment.getId(), comment.getBody(), comment.getCreatedAt(),
                    comment.getEditedAt(), comment.getTargetType(), comment.getTargetId(),
                    targets.novelId(comment), targets.novelTitle(comment), targets.chapterNumber(comment),
                    targets.chapterTitle(comment), comment.getParentCommentId(),
                    parentAuthor == null ? null : parentAuthor.getUsername());
        }).toList();
        return new ProfileActivityPage<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProfileActivityPage<ProfileActivityReaction> reactions(String username, int page, int size) {
        User profileOwner = profiles.requirePublicProfileOwner(username);
        Page<Reaction> result = reactions.findPublicActivityByUserId(profileOwner.getId(), pageRequest(page, size));
        ActivityTargets targets = findTargets(List.of(), result.getContent());
        List<ProfileActivityReaction> content = result.getContent().stream().map(reaction ->
                new ProfileActivityReaction(reaction.getId(), reaction.getReactionType(), reaction.getTargetType(),
                        reaction.getTargetId(), targets.novelId(reaction), targets.novelTitle(reaction),
                        targets.chapterNumber(reaction), targets.chapterTitle(reaction))).toList();
        return new ProfileActivityPage<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("Invalid activity page or size.");
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private Map<Long, Comment> findParents(List<Comment> activity) {
        List<Long> ids = activity.stream().map(Comment::getParentCommentId).filter(id -> id != null).distinct().toList();
        return ids.isEmpty() ? Map.of() : comments.findAllById(ids).stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));
    }

    private Map<Long, User> findParentAuthors(Collection<Comment> parents) {
        Set<Long> ids = parents.stream().map(Comment::getAuthorUserId).collect(Collectors.toSet());
        return ids.isEmpty() ? Map.of() : users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ActivityTargets findTargets(List<Comment> commentRows, List<Reaction> reactionRows) {
        List<Long> novelIds = java.util.stream.Stream.concat(
                        commentRows.stream().filter(row -> row.getTargetType() == CommentTargetType.NOVEL).map(Comment::getTargetId),
                        reactionRows.stream().filter(row -> row.getTargetType() == ReactionTargetType.NOVEL).map(Reaction::getTargetId))
                .distinct().toList();
        List<Long> chapterIds = java.util.stream.Stream.concat(
                        commentRows.stream().filter(row -> row.getTargetType() == CommentTargetType.CHAPTER).map(Comment::getTargetId),
                        reactionRows.stream().filter(row -> row.getTargetType() == ReactionTargetType.CHAPTER).map(Reaction::getTargetId))
                .distinct().toList();
        Map<Long, Novel> novelRows = novelIds.isEmpty() ? Map.of() : novels.findAllById(novelIds).stream()
                .collect(Collectors.toMap(Novel::getId, Function.identity()));
        Map<Long, PublicActivityChapterRow> chapterRows = chapterIds.isEmpty() ? Map.of()
                : chapters.findPublicActivityRowsByChapterIdIn(chapterIds).stream()
                .collect(Collectors.toMap(PublicActivityChapterRow::chapterId, Function.identity()));
        return new ActivityTargets(novelRows, chapterRows);
    }

    private record ActivityTargets(Map<Long, Novel> novels, Map<Long, PublicActivityChapterRow> chapters) {
        Long novelId(Comment comment) { return comment.getTargetType() == CommentTargetType.NOVEL ? comment.getTargetId() : chapters.get(comment.getTargetId()).novelId(); }
        String novelTitle(Comment comment) { return comment.getTargetType() == CommentTargetType.NOVEL ? novels.get(comment.getTargetId()).getTitle() : chapters.get(comment.getTargetId()).novelTitle(); }
        String chapterNumber(Comment comment) { return comment.getTargetType() == CommentTargetType.CHAPTER ? chapters.get(comment.getTargetId()).chapterNumber() : null; }
        String chapterTitle(Comment comment) { return comment.getTargetType() == CommentTargetType.CHAPTER ? chapters.get(comment.getTargetId()).chapterTitle() : null; }
        Long novelId(Reaction reaction) { return reaction.getTargetType() == ReactionTargetType.NOVEL ? reaction.getTargetId() : chapters.get(reaction.getTargetId()).novelId(); }
        String novelTitle(Reaction reaction) { return reaction.getTargetType() == ReactionTargetType.NOVEL ? novels.get(reaction.getTargetId()).getTitle() : chapters.get(reaction.getTargetId()).novelTitle(); }
        String chapterNumber(Reaction reaction) { return reaction.getTargetType() == ReactionTargetType.CHAPTER ? chapters.get(reaction.getTargetId()).chapterNumber() : null; }
        String chapterTitle(Reaction reaction) { return reaction.getTargetType() == ReactionTargetType.CHAPTER ? chapters.get(reaction.getTargetId()).chapterTitle() : null; }
    }
}
