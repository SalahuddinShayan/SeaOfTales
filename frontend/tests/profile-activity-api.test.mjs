import assert from "node:assert/strict";
import test from "node:test";
import { getProfileComments, getProfileReactions, ProfileActivityApiError } from "../src/components/profiles/profile-activity-api.ts";

test("requests bounded comment pages for an encoded profile username and preserves plain-text bodies", async (t) => {
  const originalFetch = globalThis.fetch;
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return Response.json({
      content: [{ id: 7, body: "<script>alert(1)</script>\ntext", targetType: "CHAPTER", targetId: 12, novelId: 4 }],
      page: 1, size: 10, totalElements: 11, totalPages: 2,
    });
  };
  t.after(() => { globalThis.fetch = originalFetch; });

  const page = await getProfileComments("reader name", 1, 10);
  assert.equal(request.url, "/api/profiles/reader%20name/activity/comments?page=1&size=10");
  assert.equal(request.options.credentials, "same-origin");
  assert.equal(page.content[0].body, "<script>alert(1)</script>\ntext");
  assert.equal(page.totalPages, 2);
});

test("requests separately paged current-reaction activity", async (t) => {
  const originalFetch = globalThis.fetch;
  let url;
  globalThis.fetch = async (requestUrl) => {
    url = requestUrl;
    return Response.json({ content: [{ id: 8, reactionType: "FIRE", targetType: "NOVEL", targetId: 4, novelId: 4, novelTitle: "Story" }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  };
  t.after(() => { globalThis.fetch = originalFetch; });

  const page = await getProfileReactions("reader", 0, 10);
  assert.equal(url, "/api/profiles/reader/activity/reactions?page=0&size=10");
  assert.equal(page.content[0].reactionType, "FIRE");
  assert.equal(page.content[0].novelId, 4);
});

test("turns profile activity failures into retryable errors", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => new Response(null, { status: 500 });
  await assert.rejects(getProfileComments("reader"), (error) => error instanceof ProfileActivityApiError && error.status === 500);
});
