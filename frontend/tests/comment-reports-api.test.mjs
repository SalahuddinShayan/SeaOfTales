import assert from "node:assert/strict";
import test from "node:test";
import {
  CommentReportsApiError,
  listCommentReports,
  resolveCommentReport,
} from "../src/components/admin/comment-reports-api.ts";

test("lists comment reports using the backend status and pageable response contract", async (t) => {
  const originalFetch = globalThis.fetch;
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return Response.json({ content: [], page: 1, size: 20, totalElements: 20, totalPages: 1 });
  };
  t.after(() => { globalThis.fetch = originalFetch; });

  const page = await listCommentReports("PENDING", 1, 20);
  assert.equal(request.url, "/api/admin/comment-reports?status=PENDING&page=1&size=20");
  assert.equal(request.options.credentials, "same-origin");
  assert.deepEqual(page, { content: [], page: 1, size: 20, totalElements: 20, totalPages: 1 });
});

test("resolves with supported DISMISSED or ACTIONED request values", async (t) => {
  const originalFetch = globalThis.fetch;
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push({ url, options });
    return Response.json({ id: 8, commentId: 3, status: JSON.parse(options.body).status });
  };
  t.after(() => { globalThis.fetch = originalFetch; });

  await resolveCommentReport(8, "DISMISSED");
  await resolveCommentReport(8, "ACTIONED");
  assert.deepEqual(requests.map(({ url, options }) => [url, options.method, JSON.parse(options.body)]), [
    ["/api/admin/comment-reports/8", "PATCH", { status: "DISMISSED" }],
    ["/api/admin/comment-reports/8", "PATCH", { status: "ACTIONED" }],
  ]);
});

test("reports authentication and permission failures without treating them as success", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  for (const [status, expected] of [[401, "Your admin session has expired."], [403, "You do not have permission"]]) {
    globalThis.fetch = async () => new Response(null, { status });
    await assert.rejects(listCommentReports("PENDING"), (error) => {
      assert.ok(error instanceof CommentReportsApiError);
      assert.equal(error.status, status);
      assert.ok(error.message.startsWith(expected));
      return true;
    });
  }
});
