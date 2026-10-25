"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import NovelCover, { readableTitle } from "@/components/home/NovelCover";
import { relativeTime } from "@/components/home/SeaOfTalesHome";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";
import { useAuth } from "@/components/auth/AuthProvider";
import NovelDescription from "@/components/NovelDescription";
import ReactionSection from "@/components/reactions/ReactionSection";
import CommentSection from "@/components/comments/CommentSection";

type Chapter = { id: number; novelId: number; chapterNumber: string; title: string; updatedAt: string };
type NovelPageData = {
  novel: { id: number; title: string; authorUsername: string; sourceLanguage: string; description: string | null; coverImage: string | null; status: string };
  chapters: Chapter[];
};

function displayValue(value: string) {
  return value.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export default function NovelPage() {
  const { novelId } = useParams<{ novelId: string }>();
  const [data, setData] = useState<NovelPageData | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(false);
  const { status } = useAuth();
  const [saved, setSaved] = useState(false);
  const [libraryBusy, setLibraryBusy] = useState(false);
  const [libraryMessage, setLibraryMessage] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/novels/${novelId}/public`, { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (response.status === 404) {
          setNotFound(true);
          throw new Error("Novel not found.");
        }
        if (!response.ok) throw new Error("Novel could not be loaded.");
        return response.json() as Promise<NovelPageData>;
      })
      .then(setData)
      .catch((reason: unknown) => {
        if (reason instanceof Error && reason.name === "AbortError") return;
        if (!(reason instanceof Error && reason.message === "Novel not found.")) setError(true);
      });
    return () => controller.abort();
  }, [novelId]);

  useEffect(() => {
    if (status !== "authenticated") return;
    fetch(`/api/library/novels/${novelId}`, { credentials: "same-origin", cache: "no-store" })
      .then((response) => response.ok ? response.json() as Promise<boolean> : false)
      .then(setSaved)
      .catch(() => undefined);
  }, [novelId, status]);

  async function toggleLibrary() {
    if (status !== "authenticated") { setLibraryMessage("Please log in or register to save novels to your Library."); return; }
    setLibraryBusy(true); setLibraryMessage("");
    try {
      const response = await fetch(`/api/library/novels/${novelId}`, { method: saved ? "DELETE" : "POST", credentials: "same-origin" });
      if (!response.ok) throw new Error();
      setSaved(!saved);
      setLibraryMessage(saved ? "Removed from your Library." : "Saved to your Library.");
    } catch { setLibraryMessage("Could not update your Library. Please try again."); }
    finally { setLibraryBusy(false); }
  }

  return (
    <div className="min-h-screen">
      <SiteNavbar />
      <main className="novel-page">
        {!data && !notFound && !error && <p className="novels-feedback" role="status">Finding this story at sea…</p>}
        {notFound && <section className="novel-page__state" role="alert"><h1>Novel not found</h1><p>We could not find that story.</p><Link href="/novels">Browse novels</Link></section>}
        {error && <section className="novel-page__state" role="alert"><h1>Novel could not be loaded</h1><p>Please try again later.</p><Link href="/novels">Back to novels</Link></section>}
        {data && (
          <>
            <section className="novel-hero">
              <div className="novel-hero__cover"><NovelCover novel={data.novel} sizes="(max-width: 700px) 58vw, 280px" priority /></div>
              <div className="novel-hero__copy">
                <p className="home-eyebrow">A story carried across the sea</p>
                <h1>{readableTitle(data.novel.title)}</h1>
                <p className="novel-author">By {data.novel.authorUsername}</p>
                <div className="novel-meta" aria-label="Novel details">
                  <span>{displayValue(data.novel.status)}</span>
                  <span>{displayValue(data.novel.sourceLanguage)}</span>
                </div>
                <button className="novel-secondary-link" type="button" onClick={toggleLibrary} disabled={libraryBusy}>
                  {status === "authenticated" && saved ? "Remove from Library" : "Save to Library"}
                </button>
                {libraryMessage && <p className="novel-page__feedback" role="status">{libraryMessage} {status !== "authenticated" && <><Link href="/login">Log in</Link> or <Link href="/register">register</Link>.</>}</p>}
                <NovelDescription description={data.novel.description} />
                {data.chapters.length > 0 && (
                  <div className="novel-actions">
                    <Link className="read-novel-link" href={`/novels/${novelId}/chapters/${data.chapters[0].id}`}>Read First Chapter</Link>
                    <Link className="novel-secondary-link" href={`/novels/${novelId}/chapters/${data.chapters[data.chapters.length - 1].id}`}>Read Last Chapter</Link>
                  </div>
                )}
              </div>
            </section>
            <section className="novel-chapters" aria-labelledby="novel-chapters-heading">
              <div className="novel-chapters__heading"><p className="home-eyebrow">The journey continues</p><h2 id="novel-chapters-heading">Table of Contents</h2></div>
              {data.chapters.length === 0 ? <p className="novel-page__empty">No chapters are currently available.</p> : (
                <div className="novel-chapters__list">
                  {data.chapters.map((chapter) => <Link className="novel-chapter-row" key={chapter.id} href={`/novels/${novelId}/chapters/${chapter.id}`}><span>C{chapter.chapterNumber} : {readableTitle(chapter.title)}</span><time dateTime={chapter.updatedAt}>{relativeTime(chapter.updatedAt)}</time></Link>)}
                </div>
              )}
            </section>
            <ReactionSection targetType="novel" targetId={data.novel.id} />
            <CommentSection key={`novel:${data.novel.id}`} targetType="novel" targetId={data.novel.id} />
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}
