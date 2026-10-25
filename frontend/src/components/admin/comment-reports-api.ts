export type ReportStatus = "PENDING" | "DISMISSED" | "ACTIONED";
export type ReportResolution = Exclude<ReportStatus, "PENDING">;

export type CommentReport = {
  id: number;
  commentId: number;
  category: "SPAM" | "ABUSE" | "HARASSMENT" | "SEXUAL_CONTENT" | "SPOILERS" | "OTHER";
  status: ReportStatus;
  createdAt: string;
  commentBody: string | null;
  targetType: "NOVEL" | "CHAPTER" | null;
  targetId: number | null;
};

export type CommentReportPage = {
  content: CommentReport[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type CommentReportDetail = {
  id: number;
  commentId: number;
  reporterUsername: string;
  category: CommentReport["category"];
  status: ReportStatus;
  createdAt: string;
  resolvedAt: string | null;
  resolverUsername: string | null;
  commentBody: string;
  commentAuthorUsername: string;
  targetType: "NOVEL" | "CHAPTER";
  targetId: number;
  parentCommentId: number | null;
};

export class CommentReportsApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "CommentReportsApiError";
    this.status = status;
  }
}

async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, { credentials: "same-origin", cache: "no-store", ...init });
  } catch {
    throw new CommentReportsApiError("Could not connect. Check your connection and try again.", 0);
  }
  if (!response.ok) {
    let payload: { message?: unknown } = {};
    try { payload = await response.json() as typeof payload; } catch { /* Security errors may not include JSON. */ }
    const message = response.status === 401
      ? "Your admin session has expired. Please sign in again."
      : response.status === 403
        ? "You do not have permission to access comment reports."
        : typeof payload.message === "string"
          ? payload.message
          : "Comment reports could not be loaded. Please try again.";
    throw new CommentReportsApiError(message, response.status);
  }
  return response.json() as Promise<T>;
}

export function listCommentReports(status: ReportStatus, page = 0, size = 20, signal?: AbortSignal) {
  return request<CommentReportPage>(`/api/admin/comment-reports?status=${status}&page=${page}&size=${size}`, { signal });
}

export function resolveCommentReport(reportId: number, status: ReportResolution) {
  return request<CommentReportDetail>(`/api/admin/comment-reports/${reportId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
}
