"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AuthGate from "@/components/auth/AuthGate";
import NovelCard from "@/components/catalog/NovelCard";
import type { NovelSummary } from "@/components/home/home-types";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

function LibraryContent() {
  const [novels, setNovels] = useState<NovelSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [message, setMessage] = useState("");

  function load() {
    setLoading(true); setError(false);
    fetch("/api/library", { credentials: "same-origin", cache: "no-store" })
      .then((response) => { if (!response.ok) throw new Error(); return response.json() as Promise<NovelSummary[]>; })
      .then(setNovels).catch(() => setError(true)).finally(() => setLoading(false));
  }
  useEffect(() => {
    fetch("/api/library", { credentials: "same-origin", cache: "no-store" })
      .then((response) => { if (!response.ok) throw new Error(); return response.json() as Promise<NovelSummary[]>; })
      .then(setNovels).catch(() => setError(true)).finally(() => setLoading(false));
  }, []);

  async function remove(novelId: number) {
    setMessage("");
    const response = await fetch(`/api/library/novels/${novelId}`, { method: "DELETE", credentials: "same-origin" });
    if (!response.ok) { setMessage("Could not remove that novel. Please try again."); return; }
    setNovels((current) => current.filter((novel) => novel.id !== novelId));
    setMessage("Novel removed from your Library.");
  }

  return <div className="min-h-screen"><SiteNavbar /><main><section className="home-section home-section--popular novels-section">
    <header className="novels-heading"><p className="home-eyebrow">Your saved stories</p><h1 className="text-3xl font-semibold leading-tight">Library</h1><p className="novels-intro">Keep the novels you want to return to close at hand.</p></header>
    {loading && <p className="novels-feedback" role="status">Loading your Library…</p>}
    {error && <div className="novel-page__state" role="alert"><h2>Library could not be loaded</h2><p>Please try again later.</p><button className="novel-secondary-link" type="button" onClick={load}>Try again</button></div>}
    {!loading && !error && novels.length === 0 && <div className="novel-page__state"><h2>Your Library is empty</h2><p>Save a novel to find it here later.</p><Link className="read-novel-link" href="/novels">Browse novels</Link></div>}
    {message && <p className="novels-feedback" role="status">{message}</p>}
    {!loading && !error && novels.length > 0 && <div className="novels-grid grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{novels.map((novel) => <div className="library-card" key={novel.id}><NovelCard novel={novel} /><button className="novel-secondary-link library-card__remove" type="button" onClick={() => remove(novel.id)}>Remove</button></div>)}</div>}
  </section></main><SiteFooter /></div>;
}

export default function LibraryPage() { return <AuthGate mode="authenticated"><LibraryContent /></AuthGate>; }
