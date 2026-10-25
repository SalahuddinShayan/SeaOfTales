package com.seaoftales.comment.service;

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
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_REPLIES_PER_PAGE = 200;

    private final CommentRepository comments;
    private final CommentVoteRepository votes;
    private final CommentReportRepository reports;
    private final UserRepository users;
    private final NovelRepository novels;
    private final ChapterRepository chapters;
    private final long postingCooldownSeconds;

    public CommentService(CommentRepository comments, CommentVoteRepository votes,
                          CommentReportRepository reports, UserRepository users,
                          NovelRepository novels, ChapterRepository chapters,
                          @Value("${seaoftales.comments.posting-cooldown-seconds:30}") long postingCooldownSeconds) {
        if (postingCooldownSeconds < 0) throw new IllegalArgumentException("Comment posting cooldown cannot be negative.");
        this.comments = comments;
        this.votes = votes;
        this.reports = reports;
        this.users = users;
        this.novels = novels;
        this.chapters = chapters;
        this.postingCooldownSeconds = postingCooldownSeconds;
    }

    @Transactional(readOnly = true)
    public CommentDtos.CommentPage list(CommentTargetType targetType, Long targetId, String username, int page, int size) {
        requireTarget(targetType, targetId);
        PageRequest request = pageRequest(page, size);
        Page<Comment> roots = comments.findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtAscIdAsc(
                targetType, targetId, request);
        List<Comment> topLevel = roots.getContent();
        List<Long> rootIds = topLevel.stream().map(Comment::getId).toList();
        List<Comment> replies = rootIds.isEmpty() ? List.of()
                : comments.findByParentCommentIdInOrderByParentCommentIdAscCreatedAtAscIdAsc(
                        rootIds, PageRequest.of(0, MAX_REPLIES_PER_PAGE));
        Map<Long, Long> replyCounts = rootIds.isEmpty() ? Map.of()
                : comments.countRepliesByParentCommentIdIn(rootIds).stream().collect(Collectors.toMap(
                        CommentRepository.ReplyCount::getParentCommentId,
                        CommentRepository.ReplyCount::getReplyCount));

        List<Comment> allComments = new ArrayList<>(topLevel);
        allComments.addAll(replies);
        Map<Long, CommentDtos.CommentView> views = buildCommentViews(allComments, username);
        Map<Long, List<Comment>> repliesByParent = replies.stream().collect(Collectors.groupingBy(
                Comment::getParentCommentId, HashMap::new,
                Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                        .sorted(Comparator.comparing(Comment::getCreatedAt).thenComparing(Comment::getId)).toList())));
        List<CommentDtos.CommentThread> threads = topLevel.stream().map(root -> {
            List<Comment> rootReplies = repliesByParent.getOrDefault(root.getId(), List.of());
            long replyCount = replyCounts.getOrDefault(root.getId(), 0L);
            return new CommentDtos.CommentThread(views.get(root.getId()), replyCount,
                    rootReplies.stream().map(reply -> views.get(reply.getId())).toList(), replyCount > rootReplies.size());
        }).toList();
        return new CommentDtos.CommentPage(targetType, targetId, threads, page, size,
                roots.getTotalElements(), roots.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CommentDtos.ReplyPage listReplies(CommentTargetType targetType, Long targetId, Long parentId,
                                             String username, int page, int size) {
        requireTarget(targetType, targetId);
        Comment parent = comments.findById(parentId)
                .orElseThrow(() -> CommentApiException.notFound("Comment not found."));
        if (parent.getParentCommentId() != null) throw CommentApiException.badRequest("Replies cannot have replies.");
        if (parent.getTargetType() != targetType || !parent.getTargetId().equals(targetId)) {
            throw CommentApiException.notFound("Comment not found in this discussion.");
        }
        Page<Comment> result = comments.findByParentCommentIdOrderByCreatedAtAscIdAsc(parentId, pageRequest(page, size));
        Map<Long, CommentDtos.CommentView> views = buildCommentViews(result.getContent(), username);
        return new CommentDtos.ReplyPage(parentId, result.getContent().stream()
                .map(comment -> views.get(comment.getId())).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public CommentDtos.CommentView create(String username, CommentTargetType targetType, Long targetId, String rawBody) {
        requireTarget(targetType, targetId);
        User actor = actorForWrite(username);
        String body = validateBody(rawBody);
        ensureNotDuplicate(actor.getId(), targetType, targetId, body, null);
        enforceCooldown(actor.getId());
        Comment saved = comments.saveAndFlush(new Comment(actor.getId(), targetType, targetId, null, body));
        return buildCommentViews(List.of(saved), username).get(saved.getId());
    }

    @Transactional
    public CommentDtos.CommentView reply(String username, CommentTargetType targetType, Long targetId,
                                         Long parentCommentId, String rawBody) {
        User actor = actorForWrite(username);
        Comment parent = comments.findByIdForUpdate(parentCommentId)
                .orElseThrow(() -> CommentApiException.notFound("Comment not found."));
        if (parent.getParentCommentId() != null) {
            throw CommentApiException.badRequest("Replies cannot have replies.");
        }
        if (parent.getTargetType() != targetType || !parent.getTargetId().equals(targetId)) {
            throw CommentApiException.badRequest("Reply target must match the parent comment target.");
        }
        requireTarget(parent.getTargetType(), parent.getTargetId());
        String body = validateBody(rawBody);
        ensureNotDuplicate(actor.getId(), parent.getTargetType(), parent.getTargetId(), body, null);
        enforceCooldown(actor.getId());
        Comment saved = comments.saveAndFlush(new Comment(actor.getId(), parent.getTargetType(),
                parent.getTargetId(), parent.getId(), body));
        return buildCommentViews(List.of(saved), username).get(saved.getId());
    }

    @Transactional
    public CommentDtos.CommentView edit(String username, Long commentId, String rawBody) {
        User actor = actorForWrite(username);
        Comment comment = lockComment(commentId);
        requireOwnerOrAdmin(actor, comment);
        String body = validateBody(rawBody);
        if (!comment.getBody().equals(body)) {
            ensureNotDuplicate(comment.getAuthorUserId(), comment.getTargetType(), comment.getTargetId(), body, commentId);
            comment.setBody(body);
            comment.setEditedAt(Instant.now());
            comments.saveAndFlush(comment);
        }
        return buildCommentViews(List.of(comment), username).get(comment.getId());
    }

    @Transactional
    public void delete(String username, Long commentId) {
        User actor = actorForWrite(username);
        Comment comment = lockComment(commentId);
        requireOwnerOrAdmin(actor, comment);
        if (comment.getParentCommentId() == null) {
            votes.deleteByParentOrReplyIds(commentId);
            reports.deleteByParentOrReplyIds(commentId);
            comments.deleteRepliesByParentId(commentId);
        } else {
            votes.deleteByCommentId(commentId);
            reports.deleteByCommentId(commentId);
        }
        comments.delete(comment);
        comments.flush();
    }

    @Transactional
    public CommentDtos.VoteResult toggleVote(String username, Long commentId, CommentVoteType requestedVote) {
        if (requestedVote == null) throw CommentApiException.badRequest("Vote type is required.");
        User actor = actorForWrite(username);
        Comment comment = lockComment(commentId);
        CommentVote existing = votes.findByUserIdAndCommentId(actor.getId(), commentId).orElse(null);
        if (existing == null) votes.saveAndFlush(new CommentVote(actor.getId(), commentId, requestedVote));
        else if (existing.getVoteType() == requestedVote) votes.delete(existing);
        else {
            existing.setVoteType(requestedVote);
            votes.saveAndFlush(existing);
        }
        return voteResult(commentId, actor.getId());
    }

    @Transactional
    public CommentDtos.VoteResult removeVote(String username, Long commentId) {
        User actor = actorForWrite(username);
        lockComment(commentId);
        votes.findByUserIdAndCommentId(actor.getId(), commentId).ifPresent(votes::delete);
        return voteResult(commentId, actor.getId());
    }

    @Transactional
    public CommentDtos.ReportReceipt report(String username, Long commentId, CommentReportCategory category) {
        if (category == null) throw CommentApiException.badRequest("Report category is required.");
        User actor = actorForWrite(username);
        lockComment(commentId);
        if (reports.findByUserIdAndCommentId(actor.getId(), commentId).isPresent()) {
            throw CommentApiException.conflict("duplicate_report", "You have already reported this item.");
        }
        CommentReport saved = reports.saveAndFlush(new CommentReport(actor.getId(), commentId, category));
        return new CommentDtos.ReportReceipt(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public CommentDtos.ReportPage listReports(CommentReportStatus status, int page, int size) {
        if (status == null) throw CommentApiException.badRequest("Report status is required.");
        Page<CommentReport> result = reports.findByStatusOrderByCreatedAtAscIdAsc(status, pageRequest(page, size));
        Map<Long, Comment> commentsById = findComments(result.getContent());
        Map<Long, User> usersById = findUsers(result.getContent().stream().map(CommentReport::getUserId).toList());
        List<CommentDtos.ReportItem> content = result.getContent().stream().map(report -> {
            Comment comment = commentsById.get(report.getCommentId());
            User reporter = usersById.get(report.getUserId());
            return new CommentDtos.ReportItem(report.getId(), report.getCommentId(),
                    reporter == null ? null : reporter.getUsername(), report.getCategory(), report.getStatus(),
                    report.getCreatedAt(), comment == null ? null : comment.getBody(),
                    comment == null ? null : comment.getTargetType(), comment == null ? null : comment.getTargetId());
        }).toList();
        return new CommentDtos.ReportPage(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CommentDtos.ReportDetail getReport(Long reportId) {
        CommentReport report = reports.findById(reportId)
                .orElseThrow(() -> CommentApiException.notFound("Report not found."));
        Comment comment = comments.findById(report.getCommentId())
                .orElseThrow(() -> CommentApiException.notFound("Reported comment not found."));
        Map<Long, User> usersById = findUsers(List.of(report.getUserId(), comment.getAuthorUserId(), report.getResolverUserId())
                .stream().filter(id -> id != null).toList());
        return reportDetail(report, comment, usersById);
    }

    @Transactional
    public CommentDtos.ReportDetail resolveReport(String username, Long reportId, CommentReportStatus status) {
        if (status != CommentReportStatus.DISMISSED && status != CommentReportStatus.ACTIONED) {
            throw CommentApiException.badRequest("Report status must be DISMISSED or ACTIONED.");
        }
        User actor = actorForWrite(username);
        CommentReport report = reports.findByIdForUpdate(reportId)
                .orElseThrow(() -> CommentApiException.notFound("Report not found."));
        if (report.getStatus() != CommentReportStatus.PENDING) {
            throw CommentApiException.conflict("report_already_resolved", "This report has already been resolved.");
        }
        report.setStatus(status);
        report.setResolution(Instant.now(), actor.getId());
        reports.saveAndFlush(report);
        Comment comment = comments.findById(report.getCommentId())
                .orElseThrow(() -> CommentApiException.notFound("Reported comment not found."));
        return reportDetail(report, comment, findUsers(List.of(report.getUserId(), comment.getAuthorUserId(), actor.getId())));
    }

    private CommentDtos.ReportDetail reportDetail(CommentReport report, Comment comment, Map<Long, User> userMap) {
        User reporter = userMap.get(report.getUserId());
        User resolver = report.getResolverUserId() == null ? null : userMap.get(report.getResolverUserId());
        User author = userMap.get(comment.getAuthorUserId());
        return new CommentDtos.ReportDetail(report.getId(), comment.getId(),
                reporter == null ? null : reporter.getUsername(), report.getCategory(), report.getStatus(),
                report.getCreatedAt(), report.getResolvedAt(), resolver == null ? null : resolver.getUsername(),
                comment.getBody(), author == null ? null : author.getUsername(), comment.getTargetType(),
                comment.getTargetId(), comment.getParentCommentId());
    }

    private CommentDtos.VoteResult voteResult(Long commentId, Long userId) {
        long likes = 0;
        long dislikes = 0;
        for (CommentVoteRepository.VoteCount row : votes.countByCommentId(commentId)) {
            if (row.getVoteType() == CommentVoteType.LIKE) likes = row.getVoteCount();
            if (row.getVoteType() == CommentVoteType.DISLIKE) dislikes = row.getVoteCount();
        }
        CommentVoteType current = votes.findByUserIdAndCommentId(userId, commentId)
                .map(CommentVote::getVoteType).orElse(null);
        return new CommentDtos.VoteResult(commentId, likes, dislikes, current);
    }

    private Map<Long, CommentDtos.CommentView> buildCommentViews(Collection<Comment> rows, String username) {
        List<Long> ids = rows.stream().map(Comment::getId).toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, User> authors = findUsers(rows.stream().map(Comment::getAuthorUserId).collect(Collectors.toSet()));
        Map<Long, CommentVoteType> currentVotes = new HashMap<>();
        if (username != null) {
            users.findByUsername(username).ifPresent(user -> votes.findByUserIdAndCommentIdIn(user.getId(), ids)
                    .forEach(vote -> currentVotes.put(vote.getCommentId(), vote.getVoteType())));
        }
        Map<Long, long[]> counts = new HashMap<>();
        for (CommentVoteRepository.CommentVoteCount row : votes.countByCommentIdIn(ids)) {
            long[] pair = counts.computeIfAbsent(row.getCommentId(), ignored -> new long[2]);
            pair[row.getVoteType() == CommentVoteType.LIKE ? 0 : 1] = row.getVoteCount();
        }
        Map<Long, CommentDtos.CommentView> result = new HashMap<>();
        for (Comment comment : rows) {
            User author = authors.get(comment.getAuthorUserId());
            long[] pair = counts.getOrDefault(comment.getId(), new long[2]);
            String picture = author == null || author.getProfilePicture() == null || author.getProfilePicture().isBlank()
                    ? null : "/api/profiles/" + author.getUsername() + "/picture";
            result.put(comment.getId(), new CommentDtos.CommentView(comment.getId(),
                    author == null ? "Unknown user" : author.getUsername(), picture, comment.getBody(),
                    comment.getCreatedAt(), comment.getEditedAt(), pair[0], pair[1], currentVotes.get(comment.getId())));
        }
        return result;
    }

    private Map<Long, Comment> findComments(List<CommentReport> reportRows) {
        List<Long> ids = reportRows.stream().map(CommentReport::getCommentId).distinct().toList();
        return ids.isEmpty() ? Map.of() : comments.findAllById(ids).stream().collect(Collectors.toMap(Comment::getId, Function.identity()));
    }

    private Map<Long, User> findUsers(Collection<Long> ids) {
        Set<Long> distinct = ids.stream().filter(id -> id != null).collect(Collectors.toSet());
        return distinct.isEmpty() ? Map.of() : users.findAllById(distinct).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private User actorForWrite(String username) {
        return users.findByUsernameForUpdate(username)
                .orElseThrow(() -> new CommentApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "authentication_required", "An authenticated account is required."));
    }

    private Comment lockComment(Long id) {
        if (id == null || id < 1) throw CommentApiException.badRequest("Invalid comment ID.");
        return comments.findByIdForUpdate(id).orElseThrow(() -> CommentApiException.notFound("Comment not found."));
    }

    private void requireOwnerOrAdmin(User actor, Comment comment) {
        if (actor.getRole() != com.seaoftales.identity.enums.UserRole.ADMIN
                && !actor.getId().equals(comment.getAuthorUserId())) throw CommentApiException.forbidden();
    }

    private void requireTarget(CommentTargetType type, Long id) {
        if (type == null) throw CommentApiException.badRequest("Unsupported comment target type.");
        if (id == null || id < 1) throw CommentApiException.badRequest("Invalid comment target ID.");
        boolean exists = switch (type) {
            case NOVEL -> novels.existsById(id);
            case CHAPTER -> chapters.existsPublicChapterById(id);
        };
        if (!exists) throw CommentApiException.notFound("Comment target not found.");
    }

    private String validateBody(String rawBody) {
        if (rawBody == null) throw CommentApiException.badRequest("Comment text is required.");
        String body = rawBody.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (body.isEmpty()) throw CommentApiException.badRequest("Comment text cannot be blank.");
        if (body.length() > 3000) throw CommentApiException.badRequest("Comment text cannot exceed 3,000 characters.");
        if (body.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                && codePoint != '\n' && codePoint != '\t')) {
            throw CommentApiException.badRequest("Comment text contains unsupported control characters.");
        }
        return body;
    }

    private void ensureNotDuplicate(Long authorId, CommentTargetType type, Long targetId, String body, Long excludedId) {
        boolean duplicate = excludedId == null
                ? comments.existsByAuthorUserIdAndTargetTypeAndTargetIdAndBody(authorId, type, targetId, body)
                : comments.existsByAuthorUserIdAndTargetTypeAndTargetIdAndBodyAndIdNot(authorId, type, targetId, body, excludedId);
        if (duplicate) throw CommentApiException.conflict("duplicate_content", "You have already posted this text in this discussion.");
    }

    private void enforceCooldown(Long authorId) {
        if (postingCooldownSeconds == 0) return;
        comments.findFirstByAuthorUserIdOrderByCreatedAtDescIdDesc(authorId).ifPresent(last -> {
            Instant allowedAt = last.getCreatedAt().plusSeconds(postingCooldownSeconds);
            Instant now = Instant.now();
            if (now.isBefore(allowedAt)) {
                long retry = Math.max(1, (allowedAt.toEpochMilli() - now.toEpochMilli() + 999) / 1000);
                throw CommentApiException.tooManyRequests(retry);
            }
        });
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw CommentApiException.badRequest("Page must be non-negative and size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        return PageRequest.of(page, size);
    }
}
