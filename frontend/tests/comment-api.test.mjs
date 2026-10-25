import assert from "node:assert/strict";
import test from "node:test";
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
} from "../src/components/comments/comment-api.ts";

async function captureFetch(t, response = Response.json({})) {
  const originalFetch = globalThis.fetch;
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return response.clone();
  };
  t.after(() => { globalThis.fetch = originalFetch; });
  return { get request() { return request; } };
}

test("requests bounded target comments and paged replies using the backend paths", async (t) => {
  const mock = await captureFetch(t, Response.json({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 }));
  await getComments("novel", 18, 2, 20);
  assert.equal(mock.request.url, "/api/comments/novel/18?page=2&size=20");
  assert.equal(mock.request.options.credentials, "same-origin");

  await getReplies("chapter", 31, 44, 1, 50);
  assert.equal(mock.request.url, "/api/comments/chapter/31/44/replies?page=1&size=50");
});

test("uses the actual create, reply, and edit request bodies", async (t) => {
  const mock = await captureFetch(t);
  await createComment("novel", 7, "Top level\ncomment");
  assert.equal(mock.request.url, "/api/comments/novel/7");
  assert.equal(mock.request.options.method, "POST");
  assert.deepEqual(JSON.parse(mock.request.options.body), { body: "Top level\ncomment" });

  await createReply("chapter", 9, 12, "Reply");
  assert.equal(mock.request.url, "/api/comments/chapter/9/12/replies");
  assert.deepEqual(JSON.parse(mock.request.options.body), { body: "Reply" });

  await editComment(12, "Edited");
  assert.equal(mock.request.url, "/api/comments/12");
  assert.equal(mock.request.options.method, "PATCH");
  assert.deepEqual(JSON.parse(mock.request.options.body), { body: "Edited" });
});

test("uses the backend delete, vote toggle/removal, and report contracts", async (t) => {
  const mock = await captureFetch(t, new Response(null, { status: 204 }));
  await deleteComment(5);
  assert.equal(mock.request.url, "/api/comments/5");
  assert.equal(mock.request.options.method, "DELETE");

  globalThis.fetch = async (url, options) => { mock.last = { url, options }; return Response.json({ commentId: 5, likes: 1, dislikes: 0, currentUserVote: "LIKE" }); };
  await setVote(5, "LIKE");
  assert.equal(mock.last.url, "/api/comments/5/vote");
  assert.equal(mock.last.options.method, "PUT");
  assert.deepEqual(JSON.parse(mock.last.options.body), { voteType: "LIKE" });

  globalThis.fetch = async (url, options) => { mock.last = { url, options }; return Response.json({ commentId: 5, likes: 0, dislikes: 0, currentUserVote: null }); };
  await removeVote(5);
  assert.equal(mock.last.options.method, "DELETE");

  globalThis.fetch = async (url, options) => { mock.last = { url, options }; return Response.json({ id: 3, status: "PENDING", createdAt: "2026-01-01T00:00:00Z" }); };
  await reportComment(5, "SPOILERS");
  assert.equal(mock.last.url, "/api/comments/5/reports");
  assert.deepEqual(JSON.parse(mock.last.options.body), { category: "SPOILERS" });
});

test("maps validation, duplicate, cooldown, authentication, and permission errors without success", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  for (const [status, payload, expected] of [
    [400, { code: "invalid_request", message: "The request is invalid." }, "The request is invalid."],
    [409, { code: "duplicate_content", message: "Already posted." }, "Already posted."],
    [429, { code: "posting_cooldown", message: "Wait 20 seconds." }, "Wait 20 seconds."],
    [401, {}, "Please sign in to continue."],
    [403, {}, "You do not have permission to do that."],
  ]) {
    globalThis.fetch = async () => Response.json(payload, { status });
    await assert.rejects(createComment("novel", 4, "body"), (error) => {
      assert.ok(error instanceof CommentApiError);
      assert.equal(error.status, status);
      assert.equal(error.message, expected);
      return true;
    });
  }
});

test("maps network failures to a recoverable API error", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => { throw new TypeError("offline"); };
  await assert.rejects(getComments("chapter", 3), (error) => error instanceof CommentApiError && error.status === 0);
});
