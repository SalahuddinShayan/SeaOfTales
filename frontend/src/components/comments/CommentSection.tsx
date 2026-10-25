"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import {
  CommentApiError,
  createComment,
  createReply,
  deleteComment,
  editComment,
  getComments,
  getReplies,
  removeVote,
  reportComment,
  setVote,
  type CommentReportCategory,
  type CommentTargetType,
  type CommentThread,
  type CommentView,
  type CommentVoteType,
} from "@/components/comments/comment-api";

const BODY_LIMIT = 3000;
const THREAD_PAGE_SIZE = 20;
const REPLY_PAGE_SIZE = 50;
const REPORT_CATEGORIES: { value: CommentReportCategory; label: string }[] = [
  { value: "SPAM", label: "Spam" },
  { value: "ABUSE", label: "Abuse" },
  { value: "HARASSMENT", label: "Harassment" },
  { value: "SEXUAL_CONTENT", label: "Sexual content" },
  { value: "SPOILERS", label: "Spoilers" },
  { value: "OTHER", label: "Other" },
];

type Discussion = { threads: CommentThread[]; page: number; totalPages: number; totalElements: number };

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "The comment request could not be completed. Please try again.";
}

function CharacterCount({ value }: Readonly<{ value: string }>) {
  return <span className="comment-character-count">{value.length}/{BODY_LIMIT}</span>;
}

function ProfileAvatar({ comment }: Readonly<{ comment: CommentView }>) {
  const [imageFailed, setImageFailed] = useState(false);
  return (
    <span className="comment-avatar" aria-hidden="true">
      {comment.profilePicture && !imageFailed
        ? <Image src={comment.profilePicture} alt="" width={40} height={40} unoptimized onError={() => setImageFailed(true)} />
        : comment.authorUsername.slice(0, 1).toUpperCase()}
    </span>
  );
}

function SignInPrompt({ action }: Readonly<{ action: string }>) {
  return <p className="comment-auth-prompt" role="status">Sign in to {action}. <Link href="/login">Sign in</Link> or <Link href="/register">register</Link>.</p>;
}

export default function CommentSection({ targetType, targetId }: Readonly<{ targetType: CommentTargetType; targetId: number }>) {
  const { status, user } = useAuth();
  const [discussion, setDiscussion] = useState<Discussion | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [retry, setRetry] = useState(0);
  const [body, setBody] = useState("");
  const [replyDrafts, setReplyDrafts] = useState<Record<number, string>>({});
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDraft, setEditDraft] = useState("");
  const [reportingId, setReportingId] = useState<number | null>(null);
  const [reportCategory, setReportCategory] = useState<CommentReportCategory>("SPAM");
  const [reportSubmitted, setReportSubmitted] = useState<Record<number, boolean>>({});
  const [busyKeys, setBusyKeys] = useState<Set<string>>(new Set());
  const busyRef = useRef(new Set<string>());
  const [feedback, setFeedback] = useState("");
  const [feedbackIsError, setFeedbackIsError] = useState(false);
  const [authPrompt, setAuthPrompt] = useState("");
  const [replyPageByThread, setReplyPageByThread] = useState<Record<number, number>>({});

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getComments(targetType, targetId, 0, THREAD_PAGE_SIZE, controller.signal)
      .then((result) => {
        if (!active) return;
        setDiscussion({ threads: result.content, page: result.page, totalPages: result.totalPages, totalElements: result.totalElements });
        setReplyPageByThread({});
      })
      .catch((reason: unknown) => {
        if (active && !(reason instanceof Error && reason.name === "AbortError")) setLoadError(errorMessage(reason));
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [targetType, targetId, status, user?.username, retry]);

  function promptForSignIn(action: string) {
    setFeedback("");
    setFeedbackIsError(false);
    setAuthPrompt(action);
  }

  function updateComment(id: number, update: (comment: CommentView) => CommentView) {
    setDiscussion((current) => current && ({
      ...current,
      threads: current.threads.map((thread) => ({
        ...thread,
        comment: thread.comment.id === id ? update(thread.comment) : thread.comment,
        replies: thread.replies.map((reply) => reply.id === id ? update(reply) : reply),
      })),
    }));
  }

  async function runAction(key: string, action: () => Promise<void>) {
    if (busyRef.current.has(key)) return;
    if (status !== "authenticated") {
      promptForSignIn(key.startsWith("vote") ? "vote" : key.startsWith("report") ? "report this comment" : "continue");
      return;
    }
    busyRef.current.add(key);
    setBusyKeys(new Set(busyRef.current));
    setFeedback("");
    setFeedbackIsError(false);
    setAuthPrompt("");
    try {
      await action();
    } catch (reason) {
      if (reason instanceof CommentApiError && reason.status === 401) promptForSignIn("continue");
      else { setFeedback(errorMessage(reason)); setFeedbackIsError(true); }
    } finally {
      busyRef.current.delete(key);
      setBusyKeys(new Set(busyRef.current));
    }
  }

  async function submitTopLevel(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = body.trim();
    if (!content || content.length > BODY_LIMIT || status !== "authenticated") {
      if (status !== "authenticated") promptForSignIn("join the discussion");
      return;
    }
    await runAction("create", async () => {
      const created = await createComment(targetType, targetId, content);
      setDiscussion((current) => current && ({ ...current, threads: [...current.threads, { comment: created, replies: [], replyCount: 0, hasMoreReplies: false }], totalElements: current.totalElements + 1 }));
      setBody("");
      setFeedback("Your comment was posted.");
    });
  }

  async function submitReply(event: FormEvent<HTMLFormElement>, parentId: number) {
    event.preventDefault();
    const content = (replyDrafts[parentId] ?? "").trim();
    if (!content || content.length > BODY_LIMIT || status !== "authenticated") {
      if (status !== "authenticated") promptForSignIn("reply");
      return;
    }
    await runAction(`reply:${parentId}`, async () => {
      const created = await createReply(targetType, targetId, parentId, content);
      setDiscussion((current) => current && ({ ...current, threads: current.threads.map((thread) => thread.comment.id === parentId
        ? { ...thread, replies: [...thread.replies, created], replyCount: thread.replyCount + 1 }
        : thread) }));
      setReplyDrafts((current) => ({ ...current, [parentId]: "" }));
      setFeedback("Your reply was posted.");
    });
  }

  async function loadMoreReplies(thread: CommentThread) {
    const key = `replies:${thread.comment.id}`;
    if (busyRef.current.has(key)) return;
    busyRef.current.add(key);
    setBusyKeys(new Set(busyRef.current));
    try {
      const nextPage = replyPageByThread[thread.comment.id] ?? Math.floor(thread.replies.length / REPLY_PAGE_SIZE);
      const result = await getReplies(targetType, targetId, thread.comment.id, nextPage, REPLY_PAGE_SIZE);
      setDiscussion((current) => current && ({ ...current, threads: current.threads.map((item) => {
        if (item.comment.id !== thread.comment.id) return item;
        const known = new Set(item.replies.map((reply) => reply.id));
        return { ...item, replies: [...item.replies, ...result.content.filter((reply) => !known.has(reply.id))], replyCount: result.totalElements, hasMoreReplies: result.page + 1 < result.totalPages };
      }) }));
      setReplyPageByThread((current) => ({ ...current, [thread.comment.id]: result.page + 1 }));
    } catch (reason) {
      setFeedback(errorMessage(reason));
      setFeedbackIsError(true);
    } finally {
      busyRef.current.delete(key);
      setBusyKeys(new Set(busyRef.current));
    }
  }

  async function saveEdit(commentId: number) {
    const content = editDraft.trim();
    if (!content || content.length > BODY_LIMIT) return;
    await runAction(`edit:${commentId}`, async () => {
      const updated = await editComment(commentId, content);
      updateComment(commentId, () => updated);
      setEditingId(null);
      setEditDraft("");
      setFeedback("Your changes were saved.");
    });
  }

  async function removeComment(commentId: number) {
    if (!window.confirm("Delete this comment? This cannot be undone.")) return;
    await runAction(`delete:${commentId}`, async () => {
      await deleteComment(commentId);
      setDiscussion((current) => current && ({ ...current, threads: current.threads
        .filter((thread) => thread.comment.id !== commentId)
        .map((thread) => ({ ...thread, replies: thread.replies.filter((reply) => reply.id !== commentId), replyCount: thread.replies.some((reply) => reply.id === commentId) ? Math.max(0, thread.replyCount - 1) : thread.replyCount })) }));
      setFeedback("The comment was deleted.");
    });
  }

  async function vote(commentId: number, voteType: CommentVoteType, currentVote: CommentVoteType | null) {
    await runAction(`vote:${commentId}`, async () => {
      const result = currentVote === voteType ? await removeVote(commentId) : await setVote(commentId, voteType);
      updateComment(commentId, (comment) => ({ ...comment, likes: result.likes, dislikes: result.dislikes, currentUserVote: result.currentUserVote }));
    });
  }

  async function submitReport(event: FormEvent<HTMLFormElement>, commentId: number) {
    event.preventDefault();
    await runAction(`report:${commentId}`, async () => {
      await reportComment(commentId, reportCategory);
      setReportSubmitted((current) => ({ ...current, [commentId]: true }));
      setReportingId(null);
      setFeedback("Report received. Reporting does not automatically hide this comment.");
    });
  }

  function renderComment(comment: CommentView, isReply = false) {
    const own = status === "authenticated" && user?.username.toLowerCase() === comment.authorUsername.toLowerCase();
    const admin = status === "authenticated" && user?.role.replace(/^ROLE_/, "") === "ADMIN";
    const busy = (prefix: string) => busyKeys.has(`${prefix}:${comment.id}`);
    const canEdit = own;
    const canDelete = own || admin;
    const editing = editingId === comment.id;
    return (
      <article className={`comment-item${isReply ? " comment-item--reply" : ""}`} key={comment.id}>
        <ProfileAvatar comment={comment} />
        <div className="comment-item__main">
          <header className="comment-item__header">
            <Link href={`/profiles/${encodeURIComponent(comment.authorUsername)}`} className="comment-item__author">{comment.authorUsername}</Link>
            <time dateTime={comment.createdAt}>{formatDate(comment.createdAt)}</time>
            {comment.editedAt && <span className="comment-item__edited">Edited {formatDate(comment.editedAt)}</span>}
          </header>
          {editing ? (
            <form className="comment-form comment-form--edit" onSubmit={(event) => { event.preventDefault(); void saveEdit(comment.id); }}>
              <label className="sr-only" htmlFor={`edit-${comment.id}`}>Edit comment</label>
              <textarea id={`edit-${comment.id}`} maxLength={BODY_LIMIT} value={editDraft} onChange={(event) => setEditDraft(event.target.value)} />
              <div className="comment-form__footer"><CharacterCount value={editDraft} /><div><button type="button" className="comment-button comment-button--quiet" onClick={() => { setEditingId(null); setEditDraft(""); }}>Cancel</button><button type="submit" className="comment-button" disabled={!editDraft.trim() || editDraft.length > BODY_LIMIT || busy("edit")}>Save</button></div></div>
            </form>
          ) : <p className="comment-item__body">{comment.body}</p>}
          <div className="comment-item__actions">
            <button type="button" aria-pressed={comment.currentUserVote === "LIKE"} className={comment.currentUserVote === "LIKE" ? "comment-action is-selected" : "comment-action"} disabled={busy("vote")} onClick={() => void vote(comment.id, "LIKE", comment.currentUserVote)}>Like <span>{comment.likes}</span></button>
            <button type="button" aria-pressed={comment.currentUserVote === "DISLIKE"} className={comment.currentUserVote === "DISLIKE" ? "comment-action is-selected" : "comment-action"} disabled={busy("vote")} onClick={() => void vote(comment.id, "DISLIKE", comment.currentUserVote)}>Dislike <span>{comment.dislikes}</span></button>
            {canEdit && !editing && <button type="button" className="comment-action" onClick={() => { setEditingId(comment.id); setEditDraft(comment.body); }}>Edit</button>}
            {canDelete && <button type="button" className="comment-action" disabled={busy("delete")} onClick={() => void removeComment(comment.id)}>Delete</button>}
            {status !== "loading" && !reportSubmitted[comment.id] && <button type="button" className="comment-action" onClick={() => {
              if (status !== "authenticated") { promptForSignIn("report this comment"); return; }
              setReportingId(reportingId === comment.id ? null : comment.id); setFeedback("");
            }}>Report</button>}
            {reportSubmitted[comment.id] && <span className="comment-item__reported">Reported</span>}
          </div>
          {reportingId === comment.id && (
            <form className="comment-report-form" onSubmit={(event) => void submitReport(event, comment.id)}>
              <label htmlFor={`report-${comment.id}`}>Reason for reporting</label>
              <select id={`report-${comment.id}`} value={reportCategory} onChange={(event) => setReportCategory(event.target.value as CommentReportCategory)}>{REPORT_CATEGORIES.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}</select>
              <button className="comment-button" type="submit" disabled={busy("report")}>Send report</button>
            </form>
          )}
        </div>
      </article>
    );
  }

  return (
    <section className={`comment-section comment-section--${targetType}`} aria-labelledby={`${targetType}-comments-heading`} data-comment-target={targetType} data-comment-target-id={targetId}>
      <header className="comment-section__heading"><div><p className="home-eyebrow">Reader conversation</p><h2 id={`${targetType}-comments-heading`}>Comments</h2></div>{discussion && <span>{discussion.totalElements} {discussion.totalElements === 1 ? "comment" : "comments"}</span>}</header>
      {status === "authenticated" ? (
        <form className="comment-form comment-form--new" onSubmit={(event) => void submitTopLevel(event)}>
          <label htmlFor={`${targetType}-comment-body`}>Join the discussion</label>
          <textarea id={`${targetType}-comment-body`} value={body} maxLength={BODY_LIMIT} onChange={(event) => setBody(event.target.value)} placeholder="Share a thoughtful comment…" />
          <div className="comment-form__footer"><CharacterCount value={body} /><button type="submit" className="comment-button" disabled={!body.trim() || body.length > BODY_LIMIT || busyKeys.has("create")}>Post comment</button></div>
        </form>
      ) : status === "unauthenticated" ? <SignInPrompt action="join the discussion" /> : <p className="comment-section__muted" role="status">Checking your account…</p>}
      {authPrompt && <SignInPrompt action={authPrompt} />}
      {feedback && <p className={feedbackIsError ? "comment-feedback comment-feedback--error" : "comment-feedback"} role={feedbackIsError ? "alert" : "status"}>{feedback}</p>}
      {loading && !discussion && <p className="comment-section__muted" role="status">Loading comments…</p>}
      {loadError && <div className="comment-load-error" role="alert"><p>{loadError}</p><button type="button" className="comment-button comment-button--quiet" onClick={() => { setLoadError(""); setLoading(true); setRetry((value) => value + 1); }}>Try again</button></div>}
      {!loading && discussion?.totalElements === 0 && <p className="comment-section__empty">No comments yet. Start the conversation.</p>}
      {discussion && discussion.threads.length > 0 && (
        <div className="comment-list">
          {discussion.threads.map((thread) => (
            <div className="comment-thread" key={thread.comment.id}>
              {renderComment(thread.comment)}
              {thread.replies.length > 0 && <div className="comment-replies" aria-label={`Replies to ${thread.comment.authorUsername}'s comment`}>{thread.replies.map((reply) => renderComment(reply, true))}</div>}
              {thread.hasMoreReplies && <button type="button" className="comment-load-more" disabled={busyKeys.has(`replies:${thread.comment.id}`)} onClick={() => void loadMoreReplies(thread)}>Load more replies</button>}
              {status === "authenticated" ? (
                <form className="comment-form comment-form--reply" onSubmit={(event) => void submitReply(event, thread.comment.id)}>
                  <label htmlFor={`reply-${thread.comment.id}`}>Reply to {thread.comment.authorUsername}</label>
                  <textarea id={`reply-${thread.comment.id}`} value={replyDrafts[thread.comment.id] ?? ""} maxLength={BODY_LIMIT} onChange={(event) => setReplyDrafts((current) => ({ ...current, [thread.comment.id]: event.target.value }))} placeholder="Write a reply…" />
                  <div className="comment-form__footer"><CharacterCount value={replyDrafts[thread.comment.id] ?? ""} /><button type="submit" className="comment-button" disabled={!(replyDrafts[thread.comment.id] ?? "").trim() || (replyDrafts[thread.comment.id] ?? "").length > BODY_LIMIT || busyKeys.has(`reply:${thread.comment.id}`)}>Reply</button></div>
                </form>
              ) : status === "unauthenticated" ? <button type="button" className="comment-load-more" onClick={() => promptForSignIn("reply")}>Sign in to reply</button> : null}
            </div>
          ))}
        </div>
      )}
      {discussion && discussion.page + 1 < discussion.totalPages && <button type="button" className="comment-load-more" disabled={loading} onClick={() => {
        const next = discussion.page + 1;
        setLoading(true);
        void getComments(targetType, targetId, next, THREAD_PAGE_SIZE).then((result) => setDiscussion((current) => current && ({ ...current, threads: [...current.threads, ...result.content.filter((nextThread) => !current.threads.some((thread) => thread.comment.id === nextThread.comment.id))], page: result.page, totalPages: result.totalPages, totalElements: result.totalElements })))
          .catch((reason) => { setFeedback(errorMessage(reason)); setFeedbackIsError(true); }).finally(() => setLoading(false));
      }}>Load more comments</button>}
    </section>
  );
}
