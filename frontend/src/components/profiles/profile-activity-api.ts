export type ActivityTargetType = "NOVEL" | "CHAPTER";
export type ActivityPage<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number };

export type ProfileCommentActivity = {
  id: number;
  body: string;
  createdAt: string;
  editedAt: string | null;
  targetType: ActivityTargetType;
  targetId: number;
  novelId: number;
  novelTitle: string;
  chapterNumber: string | null;
  chapterTitle: string | null;
  parentCommentId: number | null;
  parentAuthorUsername: string | null;
};

export type ProfileReactionActivity = {
  id: number;
  reactionType: "LOVE" | "ANGRY" | "LAUGH" | "SAD" | "WOW" | "FIRE";
  targetType: ActivityTargetType;
  targetId: number;
  novelId: number;
  novelTitle: string;
  chapterNumber: string | null;
  chapterTitle: string | null;
};

export class ProfileActivityApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ProfileActivityApiError";
    this.status = status;
  }
}

async function request<T>(url: string, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try { response = await fetch(url, { cache: "no-store", credentials: "same-origin", signal }); }
  catch { throw new ProfileActivityApiError("Activity could not be loaded. Check your connection and try again.", 0); }
  if (!response.ok) {
    throw new ProfileActivityApiError(response.status === 404
      ? "This profile activity is unavailable."
      : "Profile activity could not be loaded. Please try again.", response.status);
  }
  return response.json() as Promise<T>;
}

const profilePath = (username: string) => `/api/profiles/${encodeURIComponent(username)}/activity`;

export function getProfileComments(username: string, page = 0, size = 10, signal?: AbortSignal) {
  return request<ActivityPage<ProfileCommentActivity>>(`${profilePath(username)}/comments?page=${page}&size=${size}`, signal);
}

export function getProfileReactions(username: string, page = 0, size = 10, signal?: AbortSignal) {
  return request<ActivityPage<ProfileReactionActivity>>(`${profilePath(username)}/reactions?page=${page}&size=${size}`, signal);
}
