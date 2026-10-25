"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AuthGate from "@/components/auth/AuthGate";
import {
  listCommentReports,
  resolveCommentReport,
  type CommentReport,
  type ReportResolution,
  type ReportStatus,
} from "@/components/admin/comment-reports-api";

const PAGE_SIZE = 20;

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown date" : new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function AdminCommentReports() {
  const [status, setStatus] = useState<ReportStatus>("PENDING");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<{ content: CommentReport[]; page: number; totalPages: number; totalElements: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState("");
  const [busyId, setBusyId] = useState<number | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    listCommentReports(status, page, PAGE_SIZE, controller.signal)
      .then((result) => {
        if (active) setData({ content: result.content, page: result.page, totalPages: result.totalPages, totalElements: result.totalElements });
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "Comment reports could not be loaded.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [status, page, retry]);

  async function resolve(reportId: number, nextStatus: ReportResolution) {
    setBusyId(reportId);
    setError("");
    setFeedback("");
    try {
      await resolveCommentReport(reportId, nextStatus);
      setFeedback(nextStatus === "DISMISSED" ? "Report dismissed." : "Report marked as actioned.");
      setLoading(true);
      if (data && data.content.length === 1 && page > 0) setPage((current) => current - 1);
      else setRetry((value) => value + 1);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The report could not be updated.");
    } finally {
      setBusyId(null);
    }
  }

  function changeStatus(nextStatus: ReportStatus) {
    setStatus(nextStatus);
    setPage(0);
    setData(null);
    setError("");
    setLoading(true);
    setFeedback("");
  }

  return (
    <main className="admin-contact-page admin-comment-reports-page">
      <header className="admin-contact-heading">
        <Link href="/" className="site-brand">SEA OF TALES</Link>
        <p className="home-eyebrow">Administration</p>
        <h1>Comment reports</h1>
        <p>Review reported comments. Dismissing or actioning a report does not automatically hide or delete its comment.</p>
        <nav className="admin-comment-reports-nav" aria-label="Admin navigation">
          <Link href="/admin/invitations">Invitations</Link><Link href="/admin/contact">Contact inbox</Link>
        </nav>
      </header>
      {error && <div className="contact-message contact-message--error" role="alert"><p>{error}</p><button type="button" onClick={() => { setError(""); setLoading(true); setRetry((value) => value + 1); }}>Try again</button></div>}
      {feedback && <p className="admin-comment-reports-feedback" role="status">{feedback}</p>}
      <section className="admin-comment-reports-list" aria-labelledby="comment-reports-list-heading">
        <div className="admin-comment-reports-toolbar">
          <div><h2 id="comment-reports-list-heading">Reports</h2><p>{data ? `${data.totalElements} ${data.totalElements === 1 ? "report" : "reports"}` : "Review the moderation queue."}</p></div>
          <label>Status
            <select value={status} disabled={loading} onChange={(event) => changeStatus(event.target.value as ReportStatus)}>
              <option value="PENDING">Pending</option><option value="DISMISSED">Dismissed</option><option value="ACTIONED">Actioned</option>
            </select>
          </label>
        </div>
        {loading && <p className="admin-comment-reports-state" role="status">Loading reports…</p>}
        {!loading && !error && data?.content.length === 0 && <p className="admin-comment-reports-state">No {status.toLowerCase()} reports.</p>}
        {!loading && data?.content.map((report) => (
          <article className="admin-comment-report" key={report.id}>
            <header><h3>{report.category.replaceAll("_", " ")}</h3><span className={`admin-comment-report__status admin-comment-report__status--${report.status.toLowerCase()}`}>{report.status}</span></header>
            <p className="admin-comment-report__meta"><time dateTime={report.createdAt}>{formatDate(report.createdAt)}</time><span>Comment #{report.commentId}</span>{report.targetType && report.targetId && <span>{report.targetType.toLowerCase()} #{report.targetId}</span>}</p>
            <blockquote>{report.commentBody ?? "The reported comment is no longer available."}</blockquote>
            {report.status === "PENDING" && <div className="admin-comment-report__actions"><button type="button" disabled={busyId === report.id} onClick={() => void resolve(report.id, "DISMISSED")}>Dismiss</button><button type="button" disabled={busyId === report.id} onClick={() => void resolve(report.id, "ACTIONED")}>Mark actioned</button></div>}
          </article>
        ))}
        {data && data.totalPages > 1 && <nav className="admin-comment-reports-pagination" aria-label="Report pages"><button type="button" disabled={loading || page === 0} onClick={() => { setError(""); setLoading(true); setPage((current) => current - 1); }}>Previous</button><span>Page {data.page + 1} of {data.totalPages}</span><button type="button" disabled={loading || page + 1 >= data.totalPages} onClick={() => { setError(""); setLoading(true); setPage((current) => current + 1); }}>Next</button></nav>}
      </section>
    </main>
  );
}

export default function AdminCommentReportsPage() {
  return <AuthGate mode="admin"><AdminCommentReports /></AuthGate>;
}
