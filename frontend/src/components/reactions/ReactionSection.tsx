"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import {
  getReactionSummary,
  toggleReaction,
  type ReactionSummary as ReactionSummaryData,
  type ReactionTargetType,
  type ReactionType,
} from "@/components/reactions/reaction-api";

export const STANDARD_REACTIONS = [
  { id: "love", label: "Love", emoji: "❤️" },
  { id: "angry", label: "Angry", emoji: "😡" },
  { id: "laugh", label: "Laugh", emoji: "😂" },
  { id: "sad", label: "Sad", emoji: "😢" },
  { id: "wow", label: "Wow", emoji: "😲" },
  { id: "fire", label: "Fire", emoji: "🔥" },
] as const satisfies ReadonlyArray<{ id: ReactionType; label: string; emoji: string }>;

type ReactionSectionProps = {
  targetType: ReactionTargetType;
  targetId: number;
};

export default function ReactionSection({ targetType, targetId }: ReactionSectionProps) {
  const { status } = useAuth();
  const [loadedResult, setLoadedResult] = useState<{ key: string; summary?: ReactionSummaryData; error?: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<{ key: string; text: string; isError: boolean } | null>(null);
  const [authPrompt, setAuthPrompt] = useState(false);
  const requestInProgress = useRef(false);
  const requestKey = `${targetType}:${targetId}:${status}`;
  const loading = loadedResult?.key !== requestKey;
  const summary = loadedResult?.key === requestKey ? loadedResult.summary ?? null : null;
  const loadError = loadedResult?.key === requestKey ? loadedResult.error ?? "" : "";
  const error = loadError || (feedback?.key === requestKey && feedback.isError ? feedback.text : "");
  const message = feedback?.key === requestKey && !feedback.isError ? feedback.text : "";

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getReactionSummary(targetType, targetId, controller.signal)
      .then((nextSummary) => { if (active) setLoadedResult({ key: requestKey, summary: nextSummary }); })
      .catch((reason: unknown) => {
        if (active && !(reason instanceof Error && reason.name === "AbortError")) {
          setLoadedResult({ key: requestKey, error: "Reactions could not be loaded. Please try again later." });
        }
      });
    return () => { active = false; controller.abort(); };
  }, [targetType, targetId, status, requestKey]);

  async function selectReaction(reaction: ReactionType) {
    if (status === "loading" || loading || busy || requestInProgress.current) return;
    if (status !== "authenticated") {
      setAuthPrompt(true);
      return;
    }
    requestInProgress.current = true;
    setBusy(true);
    setAuthPrompt(false);
    try {
      const nextSummary = await toggleReaction(targetType, targetId, reaction);
      setLoadedResult({ key: requestKey, summary: nextSummary });
      setFeedback({
        key: requestKey,
        text: nextSummary.currentUserReaction ? "Your reaction was updated." : "Your reaction was removed.",
        isError: false,
      });
    } catch (reason) {
      setFeedback({
        key: requestKey,
        text: reason instanceof Error ? reason.message : "Reactions could not be updated. Please try again.",
        isError: true,
      });
    } finally {
      requestInProgress.current = false;
      setBusy(false);
    }
  }

  const selectedReaction = summary?.currentUserReaction ?? null;
  const totalText = loading
    ? "Loading reactions…"
    : error && !summary
      ? "Reaction totals are unavailable."
      : summary?.total === 0
        ? "No reactions yet."
        : `${summary?.total ?? 0} ${(summary?.total ?? 0) === 1 ? "reaction" : "reactions"}`;

  return (
    <section
      className={`reaction-section reaction-section--${targetType}`}
      aria-labelledby={`${targetType}-reaction-heading`}
      data-reaction-target={targetType}
      data-reaction-target-id={targetId}
    >
      <div className="reaction-section__heading">
        <h2 id={`${targetType}-reaction-heading`}>Reactions</h2>
        <p>{totalText}</p>
      </div>

      <div className="reaction-section__choices" role="group" aria-label={`React to this ${targetType}`}>
        {STANDARD_REACTIONS.map((reaction) => {
          const count = summary?.counts[reaction.id];
          const selected = selectedReaction === reaction.id;
          return (
            <button
              key={reaction.id}
              type="button"
              className={`reaction-choice${selected ? " reaction-choice--selected" : ""}`}
              aria-label={`${reaction.label}, ${count === undefined ? "count unavailable" : `${count} reactions`}${selected ? ", selected" : ""}`}
              aria-pressed={selected}
              disabled={status === "loading" || loading || busy}
              onClick={() => void selectReaction(reaction.id)}
            >
              <span className="reaction-choice__emoji" aria-hidden="true">{reaction.emoji}</span>
              <span className="reaction-choice__label">{reaction.label}</span>
              <span className="reaction-choice__count">{count === undefined ? "—" : count}</span>
              {selected && <span className="reaction-choice__selected-mark" aria-hidden="true">✓</span>}
            </button>
          );
        })}
      </div>

      {loading && <p className="reaction-section__note" role="status">Loading reaction totals…</p>}
      {error && <p className="reaction-section__note" role="alert">{error}</p>}
      {message && <p className="reaction-section__note" role="status">{message}</p>}
      {status === "unauthenticated" && (
        <p className="reaction-section__note">
          {authPrompt ? "Sign in to save a reaction. " : "Sign in to choose a reaction. "}
          <Link href="/login">Sign in</Link> or <Link href="/register">register</Link>.
        </p>
      )}
      {status === "authenticated" && !error && !message && !loading && (
        <p className="reaction-section__note">
          {selectedReaction ? `Your reaction is saved to this ${targetType}.` : `Choose a reaction for this ${targetType}.`}
        </p>
      )}
    </section>
  );
}
