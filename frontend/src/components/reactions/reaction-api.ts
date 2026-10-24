export const REACTION_TYPES = ["love", "angry", "laugh", "sad", "wow", "fire"] as const;
export type ReactionType = (typeof REACTION_TYPES)[number];
export type ReactionTargetType = "novel" | "chapter";

export type ReactionSummary = {
  counts: Record<ReactionType, number>;
  total: number;
  currentUserReaction: ReactionType | null;
};

type ApiReactionType = "LOVE" | "ANGRY" | "LAUGH" | "SAD" | "WOW" | "FIRE";
type ApiReactionSummary = {
  counts?: Partial<Record<ApiReactionType, number>>;
  total?: number;
  currentUserReaction?: ApiReactionType | null;
};

const apiTypeByReaction: Record<ReactionType, ApiReactionType> = {
  love: "LOVE",
  angry: "ANGRY",
  laugh: "LAUGH",
  sad: "SAD",
  wow: "WOW",
  fire: "FIRE",
};

function fromApiType(value: ApiReactionType | null | undefined): ReactionType | null {
  if (!value) return null;
  const match = REACTION_TYPES.find((reaction) => apiTypeByReaction[reaction] === value);
  return match ?? null;
}

export function fromApiSummary(value: ApiReactionSummary): ReactionSummary {
  const counts = Object.fromEntries(
    REACTION_TYPES.map((reaction) => {
      const count = value.counts?.[apiTypeByReaction[reaction]];
      return [reaction, typeof count === "number" && Number.isFinite(count) && count > 0 ? Math.floor(count) : 0];
    }),
  ) as Record<ReactionType, number>;
  const total = typeof value.total === "number" && Number.isFinite(value.total) && value.total >= 0
    ? Math.floor(value.total)
    : Object.values(counts).reduce((sum, count) => sum + count, 0);
  return { counts, total, currentUserReaction: fromApiType(value.currentUserReaction) };
}

async function requestSummary(url: string, init?: RequestInit): Promise<ReactionSummary> {
  const response = await fetch(url, {
    cache: "no-store",
    credentials: "same-origin",
    ...init,
  });
  if (!response.ok) throw new Error("Reactions could not be updated. Please try again.");
  return fromApiSummary(await response.json() as ApiReactionSummary);
}

export function getReactionSummary(targetType: ReactionTargetType, targetId: number, signal?: AbortSignal) {
  return requestSummary(`/api/reactions/${targetType}/${targetId}`, { signal });
}

export function toggleReaction(targetType: ReactionTargetType, targetId: number, reaction: ReactionType) {
  return requestSummary(`/api/reactions/${targetType}/${targetId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reactionType: apiTypeByReaction[reaction] }),
  });
}
