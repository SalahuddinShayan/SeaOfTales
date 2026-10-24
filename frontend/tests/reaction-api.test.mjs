import assert from "node:assert/strict";
import test from "node:test";
import { fromApiSummary, getReactionSummary, toggleReaction } from "../src/components/reactions/reaction-api.ts";

const emptyCounts = { LOVE: 0, ANGRY: 0, LAUGH: 0, SAD: 0, WOW: 0, FIRE: 0 };

test("maps counts, zero-count types, totals, and selected reaction from the public API", () => {
  const summary = fromApiSummary({
    counts: { ...emptyCounts, LOVE: 4, FIRE: 2 },
    total: 6,
    currentUserReaction: "FIRE",
  });
  assert.deepEqual(summary, {
    counts: { love: 4, angry: 0, laugh: 0, sad: 0, wow: 0, fire: 2 },
    total: 6,
    currentUserReaction: "fire",
  });
});

test("fetches a public target summary with session credentials", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return Response.json({ counts: emptyCounts, total: 0, currentUserReaction: null });
  };

  const summary = await getReactionSummary("chapter", 27);
  assert.equal(request.url, "/api/reactions/chapter/27");
  assert.equal(request.options.credentials, "same-origin");
  assert.equal(summary.total, 0);
});

test("sends a toggle request and maps its persisted response", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return Response.json({ counts: { ...emptyCounts, LOVE: 1 }, total: 1, currentUserReaction: "LOVE" });
  };

  const summary = await toggleReaction("novel", 8, "love");
  assert.equal(request.url, "/api/reactions/novel/8");
  assert.equal(request.options.method, "PUT");
  assert.deepEqual(JSON.parse(request.options.body), { reactionType: "LOVE" });
  assert.equal(summary.currentUserReaction, "love");
  assert.equal(summary.counts.love, 1);
});

test("maps a successful toggle response that removes the selected reaction", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => Response.json({ counts: emptyCounts, total: 0, currentUserReaction: null });

  const summary = await toggleReaction("chapter", 27, "love");
  assert.equal(summary.total, 0);
  assert.equal(summary.currentUserReaction, null);
  assert.ok(Object.values(summary.counts).every((count) => count === 0));
});

test("rejects API failures so the UI cannot report a false success", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => new Response(null, { status: 500 });
  await assert.rejects(toggleReaction("novel", 8, "fire"), /could not be updated/);
});
