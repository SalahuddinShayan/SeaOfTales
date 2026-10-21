"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";
import NovelCover, { readableTitle } from "@/components/home/NovelCover";
import { relativeTime } from "@/components/home/SeaOfTalesHome";

type Chapter = { id: number; novelId: number; chapterNumber: string; title: string; updatedAt: string };
type Update = { novel: { id: number; title: string; status: string; sourceLanguage: string; description: string; coverImage: string | null }; latestUpdatedAt: string; recentChapters: Chapter[] };
type UpdatesResponse = { content: Update[]; page: number; size: number; totalElements: number; totalPages: number };

function UpdatesContent() {
  const router = useRouter(); const pathname = usePathname(); const params = useSearchParams();
  const page = Math.max(0, Number.parseInt(params.get("page") ?? "0", 10) || 0);
  const [data, setData] = useState<UpdatesResponse | null>(null); const [error, setError] = useState(false);
  useEffect(() => { const controller = new AbortController(); fetch(`/api/updates?page=${page}`, { cache: "no-store", signal: controller.signal }).then(r => { if (!r.ok) throw new Error(); return r.json(); }).then((updates: UpdatesResponse) => { setError(false); setData(updates); }).catch(e => { if (e.name !== "AbortError") setError(true); }); return () => controller.abort(); }, [page]);
  function go(next: number) { router.push(next > 0 ? `${pathname}?page=${next}` : pathname); }
  return <><SiteNavbar /><main className="home-section home-section--updates updates-page"><header className="updates-page__heading"><p className="home-eyebrow">Fresh from the tide</p><h1 className="text-3xl font-semibold leading-tight">Latest Updates</h1><p className="updates-page__intro">Browse the latest updates from across Sea of Tales.</p></header>{!data && !error && <p className="novels-feedback" role="status">Loading updates…</p>}{error && <p className="novels-feedback" role="alert">Updates could not be loaded right now.</p>}{data && data.content.length === 0 && <div className="novels-empty"><h2>No updates yet.</h2><p>New chapter updates will appear here.</p></div>}{data && data.content.length > 0 && <><div className="updates-grid">{data.content.map(({ novel, recentChapters }) => <article className="update-item" key={novel.id}><Link className="update-cover" href={`/novels/${novel.id}`} aria-label={`Open ${readableTitle(novel.title)}`}><NovelCover novel={novel} sizes="72px" /></Link><div className="update-body"><Link className="update-title" href={`/novels/${novel.id}`}>{readableTitle(novel.title)}</Link><div className="update-chapters">{recentChapters.map((chapter) => <Link className="update-chapter" key={`${novel.id}-${chapter.id}`} href={`/novels/${novel.id}/chapters/${chapter.id}`} title={`Chapter ${chapter.chapterNumber}: ${chapter.title}`}><span className="update-chapter__name">C{chapter.chapterNumber}: {readableTitle(chapter.title)}</span><time dateTime={chapter.updatedAt}>{relativeTime(chapter.updatedAt)}</time></Link>)}</div></div></article>)}</div>{data.totalPages > 1 && <nav className="novels-pagination" aria-label="Update pages"><button type="button" disabled={page===0} onClick={()=>go(page-1)}>Previous</button><span className="novels-pagination__group">Page {page+1} of {data.totalPages}</span><button type="button" disabled={page>=data.totalPages-1} onClick={()=>go(page+1)}>Next</button></nav>}</>}</main><SiteFooter /></>;
}

export default function UpdatesPage() { return <Suspense fallback={<><SiteNavbar /><main className="home-section home-section--updates updates-page"><p className="novels-feedback">Loading updates…</p></main><SiteFooter /></>}><UpdatesContent /></Suspense>; }
