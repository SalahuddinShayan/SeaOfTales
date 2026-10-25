"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";
import ReactionSection from "@/components/reactions/ReactionSection";
import CommentSection from "@/components/comments/CommentSection";

type ReaderData = {
  novel: { id: number; title: string };
  chapter: { id: number; novelId: number; chapterNumber: string; title: string; content: string; updatedAt: string };
  navigation: { previousChapterId: number | null; nextChapterId: number | null };
};

const TEXT_SIZES = [90, 100, 115] as const;

function displayTitle(value: string) {
  return value.replace(/<[^>]*>/g, "").trim();
}

function ReaderControls({ data, textSize, setTextSize }: Readonly<{ data: ReaderData; textSize: number; setTextSize: (size: number) => void }>) {
  const { novelId } = useParams<{ novelId: string }>();
  const { previousChapterId, nextChapterId } = data.navigation;
  return (
    <div className="reader-controls" aria-label="Reader controls">
      <div className="reader-size-controls" aria-label="Text size">
        <button type="button" onClick={() => setTextSize(TEXT_SIZES[Math.max(0, TEXT_SIZES.indexOf(textSize as typeof TEXT_SIZES[number]) - 1)])} disabled={textSize === TEXT_SIZES[0]} aria-label="Decrease text size">A−</button>
        <button type="button" onClick={() => setTextSize(100)} disabled={textSize === 100}>Default</button>
        <button type="button" onClick={() => setTextSize(TEXT_SIZES[Math.min(TEXT_SIZES.length - 1, TEXT_SIZES.indexOf(textSize as typeof TEXT_SIZES[number]) + 1)])} disabled={textSize === TEXT_SIZES[TEXT_SIZES.length - 1]} aria-label="Increase text size">A+</button>
      </div>
      <nav className="reader-navigation" aria-label="Chapter navigation">
        {previousChapterId ? <Link href={`/novels/${novelId}/chapters/${previousChapterId}`}>← Previous</Link> : <span className="reader-navigation__disabled">← Previous</span>}
        <Link href={`/novels/${novelId}`}>Novel</Link>
        {nextChapterId ? <Link href={`/novels/${novelId}/chapters/${nextChapterId}`}>Next →</Link> : <span className="reader-navigation__disabled">Next →</span>}
      </nav>
    </div>
  );
}

export default function ChapterReaderPage() {
  const { novelId, chapterId } = useParams<{ novelId: string; chapterId: string }>();
  const [data, setData] = useState<ReaderData | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(false);
  const [textSize, setTextSize] = useState(100);

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/novels/${novelId}/chapters/${chapterId}/public`, { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (response.status === 404) {
          setNotFound(true);
          throw new Error("Chapter not found.");
        }
        if (!response.ok) throw new Error("Chapter could not be loaded.");
        return response.json() as Promise<ReaderData>;
      })
      .then(setData)
      .catch((reason: unknown) => {
        if (reason instanceof Error && reason.name === "AbortError") return;
        if (!(reason instanceof Error && reason.message === "Chapter not found.")) setError(true);
      });
    return () => controller.abort();
  }, [novelId, chapterId]);

  return (
    <div className="min-h-screen">
      <SiteNavbar />
      <main className="reader-page">
        {!data && !notFound && !error && <p className="novels-feedback" role="status">Opening the chapter…</p>}
        {(notFound || error) && <section className="novel-page__state" role="alert"><h1>{notFound ? "Chapter not found" : "Chapter could not be loaded"}</h1><p>{notFound ? "This chapter is not currently available to read." : "Please try again later."}</p><Link href={`/novels/${novelId}`}>Return to novel</Link></section>}
        {data && (
          <>
            <section className="reader-content-section" aria-label="Chapter content">
              <header className="reader-heading">
                <nav className="reader-breadcrumb" aria-label="Breadcrumb"><Link href="/">Sea of Tales</Link><span>/</span><Link href={`/novels/${novelId}`}>{displayTitle(data.novel.title)}</Link><span>/</span><span>Chapter {data.chapter.chapterNumber}</span></nav>
                <p className="home-eyebrow">Now reading</p>
                <h1>{displayTitle(data.novel.title)} — Chapter {data.chapter.chapterNumber}</h1>
                <h2>{displayTitle(data.chapter.title)}</h2>
              </header>
              <ReaderControls data={data} textSize={textSize} setTextSize={setTextSize} />
              <article className="reader-content" style={{ fontSize: `${textSize}%` }} dangerouslySetInnerHTML={{ __html: data.chapter.content }} />
              <ReaderControls data={data} textSize={textSize} setTextSize={setTextSize} />
            </section>
            <ReactionSection targetType="chapter" targetId={data.chapter.id} />
            <CommentSection key={`chapter:${data.chapter.id}`} targetType="chapter" targetId={data.chapter.id} />
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}
