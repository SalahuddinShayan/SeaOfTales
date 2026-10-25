"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  getProfileComments,
  getProfileReactions,
  type ActivityPage,
  type ProfileCommentActivity,
  type ProfileReactionActivity,
} from "@/components/profiles/profile-activity-api";

const PAGE_SIZE = 10;
const REACTIONS = {
  LOVE: { label: "Love", emoji: "❤️" },
  ANGRY: { label: "Angry", emoji: "😡" },
  LAUGH: { label: "Laugh", emoji: "😂" },
  SAD: { label: "Sad", emoji: "😢" },
  WOW: { label: "Wow", emoji: "😲" },
  FIRE: { label: "Fire", emoji: "🔥" },
} as const;

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function TargetLink({ item }: Readonly<{ item: Pick<ProfileCommentActivity, "targetType" | "targetId" | "novelId" | "novelTitle" | "chapterNumber" | "chapterTitle"> }>) {
  const href = item.targetType === "NOVEL" ? `/novels/${item.novelId}` : `/novels/${item.novelId}/chapters/${item.targetId}`;
  const label = item.targetType === "NOVEL"
    ? item.novelTitle
    : `${item.novelTitle} · Chapter ${item.chapterNumber}${item.chapterTitle ? `: ${item.chapterTitle}` : ""}`;
  return <Link href={href}>{label}</Link>;
}

function appendUnique<T extends { id: number }>(current: T[], next: T[]) {
  const ids = new Set(current.map((item) => item.id));
  return [...current, ...next.filter((item) => !ids.has(item.id))];
}

export default function ProfileActivitySection({ username }: Readonly<{ username: string }>) {
  const [comments, setComments] = useState<ActivityPage<ProfileCommentActivity> | null>(null);
  const [reactions, setReactions] = useState<ActivityPage<ProfileReactionActivity> | null>(null);
  const [commentsLoading, setCommentsLoading] = useState(true);
  const [reactionsLoading, setReactionsLoading] = useState(true);
  const [commentsError, setCommentsError] = useState("");
  const [reactionsError, setReactionsError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getProfileComments(username, 0, PAGE_SIZE, controller.signal)
      .then((result) => { if (active) setComments(result); })
      .catch((reason: unknown) => { if (active && !(reason instanceof Error && reason.name === "AbortError")) setCommentsError(reason instanceof Error ? reason.message : "Comments could not be loaded."); })
      .finally(() => { if (active) setCommentsLoading(false); });
    getProfileReactions(username, 0, PAGE_SIZE, controller.signal)
      .then((result) => { if (active) setReactions(result); })
      .catch((reason: unknown) => { if (active && !(reason instanceof Error && reason.name === "AbortError")) setReactionsError(reason instanceof Error ? reason.message : "Reactions could not be loaded."); })
      .finally(() => { if (active) setReactionsLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [username, retry]);

  function retryActivity() {
    setCommentsError(""); setReactionsError(""); setCommentsLoading(true); setReactionsLoading(true); setRetry((value) => value + 1);
  }

  async function loadMoreComments() {
    if (!comments || commentsLoading) return;
    setCommentsLoading(true); setCommentsError("");
    try {
      const result = await getProfileComments(username, comments.page + 1, PAGE_SIZE);
      setComments((current) => current && ({ ...result, content: appendUnique(current.content, result.content) }));
    } catch (reason) { setCommentsError(reason instanceof Error ? reason.message : "More comments could not be loaded."); }
    finally { setCommentsLoading(false); }
  }

  async function loadMoreReactions() {
    if (!reactions || reactionsLoading) return;
    setReactionsLoading(true); setReactionsError("");
    try {
      const result = await getProfileReactions(username, reactions.page + 1, PAGE_SIZE);
      setReactions((current) => current && ({ ...result, content: appendUnique(current.content, result.content) }));
    } catch (reason) { setReactionsError(reason instanceof Error ? reason.message : "More reactions could not be loaded."); }
    finally { setReactionsLoading(false); }
  }

  return (
    <section className="profile-activity" aria-labelledby="profile-activity-heading">
      <header className="profile-activity__heading"><p className="home-eyebrow">Across the stories</p><h2 id="profile-activity-heading">Activity</h2></header>
      <section className="profile-activity__subsection" aria-labelledby="profile-comments-heading">
        <h3 id="profile-comments-heading">Comments</h3>
        {commentsLoading && !comments && <p className="profile-activity__state" role="status">Loading comments…</p>}
        {commentsError && <div className="profile-activity__error" role="alert"><p>{commentsError}</p><button type="button" onClick={retryActivity}>Try again</button></div>}
        {!commentsLoading && !commentsError && comments?.content.length === 0 && <p className="profile-activity__state">No comments yet.</p>}
        {comments && comments.content.length > 0 && <div className="profile-activity__list">{comments.content.map((comment) => (
          <article className="profile-activity-comment" key={comment.id}>
            <p className="profile-activity-comment__body">{comment.body}</p>
            <p className="profile-activity-comment__meta"><time dateTime={comment.createdAt}>{formatDate(comment.createdAt)}</time>{comment.editedAt && <span>Edited {formatDate(comment.editedAt)}</span>}<span>{comment.parentCommentId ? "Reply" : "Comment"}</span></p>
            <p className="profile-activity-comment__context">
              {comment.parentCommentId && <span>Reply to {comment.parentAuthorUsername ? `@${comment.parentAuthorUsername}` : "a comment"} in </span>}
              <TargetLink item={comment} />
            </p>
          </article>
        ))}</div>}
        {comments && !commentsError && comments.page + 1 < comments.totalPages && <button type="button" className="profile-activity__more" disabled={commentsLoading} onClick={() => void loadMoreComments()}>{commentsLoading ? "Loading…" : "Load more comments"}</button>}
        {commentsError && comments && comments.page + 1 < comments.totalPages && <button type="button" className="profile-activity__more" onClick={() => void loadMoreComments()}>Retry loading comments</button>}
      </section>
      <section className="profile-activity__subsection" aria-labelledby="profile-reactions-heading">
        <h3 id="profile-reactions-heading">Reactions</h3>
        <p className="profile-activity__note">These are current reactions, not a complete history.</p>
        {reactionsLoading && !reactions && <p className="profile-activity__state" role="status">Loading reactions…</p>}
        {reactionsError && <div className="profile-activity__error" role="alert"><p>{reactionsError}</p><button type="button" onClick={retryActivity}>Try again</button></div>}
        {!reactionsLoading && !reactionsError && reactions?.content.length === 0 && <p className="profile-activity__state">No reactions yet.</p>}
        {reactions && reactions.content.length > 0 && <div className="profile-activity__list">{reactions.content.map((reaction) => {
          const detail = REACTIONS[reaction.reactionType];
          return <article className="profile-activity-reaction" key={reaction.id}>
            <span className="profile-activity-reaction__emoji" aria-hidden="true">{detail.emoji}</span>
            <div><p><strong>Current {detail.label} reaction</strong></p><p className="profile-activity-comment__context"><TargetLink item={reaction} /></p></div>
          </article>;
        })}</div>}
        {reactions && !reactionsError && reactions.page + 1 < reactions.totalPages && <button type="button" className="profile-activity__more" disabled={reactionsLoading} onClick={() => void loadMoreReactions()}>{reactionsLoading ? "Loading…" : "Load more reactions"}</button>}
        {reactionsError && reactions && reactions.page + 1 < reactions.totalPages && <button type="button" className="profile-activity__more" onClick={() => void loadMoreReactions()}>Retry loading reactions</button>}
      </section>
    </section>
  );
}
