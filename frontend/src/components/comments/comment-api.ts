export type CommentTargetType = "novel" | "chapter";
export type CommentVoteType = "LIKE" | "DISLIKE";
export type CommentReportCategory = "SPAM" | "ABUSE" | "HARASSMENT" | "SEXUAL_CONTENT" | "SPOILERS" | "OTHER";

export type CommentView = {
  id: number;
  authorUsername: string;
  profilePicture: string | null;
  body: string;
  createdAt: string;
  editedAt: string | null;
  likes: number;
  dislikes: number;
  currentUserVote: CommentVoteType | null;
};

export type CommentThread = {
  comment: CommentView;
  replyCount: number;
  replies: CommentView[];
  hasMoreReplies: boolean;
};

export type CommentPage = {
  targetType: "NOVEL" | "CHAPTER";
  targetId: number;
  content: CommentThread[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ReplyPage = {
  parentCommentId: number;
  content: CommentView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type VoteResult = {
  commentId: number;
  likes: number;
  dislikes: number;
  currentUserVote: CommentVoteType | null;
};

export type ReportReceipt = { id: number; status: "PENDING"; createdAt: string };

export class CommentApiError extends Error {
  readonly status: number;
  readonly code: string | null;

  constructor(message: string, status: number, code: string | null = null) {
    super(message);
    this.name = "CommentApiError";
    this.status = status;
    this.code = code;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      cache: "no-store",
      credentials: "same-origin",
      ...init,
      headers: init.body ? { "Content-Type": "application/json", ...init.headers } : init.headers,
    });
  } catch {
    throw new CommentApiError("Could not connect. Check your connection and try again.", 0, "network_error");
  }

  if (!response.ok) {
    let payload: { message?: unknown; code?: unknown } = {};
    try { payload = await response.json() as typeof payload; } catch { /* Some security responses have no JSON body. */ }
    const serverMessage = typeof payload.message === "string" ? payload.message : "";
    const message = response.status === 401
      ? "Please sign in to continue."
      : response.status === 403
        ? "You do not have permission to do that."
        : response.status === 409
          ? serverMessage || "This action conflicts with an existing request."
          : response.status === 429
            ? serverMessage || "You are posting too quickly. Please wait and try again."
            : serverMessage || "The comment request could not be completed. Please try again.";
    throw new CommentApiError(message, response.status, typeof payload.code === "string" ? payload.code : null);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function targetPath(targetType: CommentTargetType, targetId: number) {
  return `/api/comments/${targetType}/${targetId}`;
}

export function getComments(targetType: CommentTargetType, targetId: number, page = 0, size = 20, signal?: AbortSignal) {
  return request<CommentPage>(`${targetPath(targetType, targetId)}?page=${page}&size=${size}`, { signal });
}

export function getReplies(targetType: CommentTargetType, targetId: number, parentId: number, page: number, size = 50, signal?: AbortSignal) {
  return request<ReplyPage>(`${targetPath(targetType, targetId)}/${parentId}/replies?page=${page}&size=${size}`, { signal });
}

export function createComment(targetType: CommentTargetType, targetId: number, body: string) {
  return request<CommentView>(targetPath(targetType, targetId), { method: "POST", body: JSON.stringify({ body }) });
}

export function createReply(targetType: CommentTargetType, targetId: number, parentId: number, body: string) {
  return request<CommentView>(`${targetPath(targetType, targetId)}/${parentId}/replies`, { method: "POST", body: JSON.stringify({ body }) });
}

export function editComment(commentId: number, body: string) {
  return request<CommentView>(`/api/comments/${commentId}`, { method: "PATCH", body: JSON.stringify({ body }) });
}

export function deleteComment(commentId: number) {
  return request<void>(`/api/comments/${commentId}`, { method: "DELETE" });
}

export function setVote(commentId: number, voteType: CommentVoteType) {
  return request<VoteResult>(`/api/comments/${commentId}/vote`, { method: "PUT", body: JSON.stringify({ voteType }) });
}

export function removeVote(commentId: number) {
  return request<VoteResult>(`/api/comments/${commentId}/vote`, { method: "DELETE" });
}

export function reportComment(commentId: number, category: CommentReportCategory) {
  return request<ReportReceipt>(`/api/comments/${commentId}/reports`, { method: "POST", body: JSON.stringify({ category }) });
}
