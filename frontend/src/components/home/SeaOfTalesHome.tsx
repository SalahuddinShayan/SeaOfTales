"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import NovelCover, { readableTitle } from "@/components/home/NovelCover";
import type { ChapterSummary, NovelSummary, PublicHomeData } from "@/components/home/home-types";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

function displayValue(value: string) {
  return value.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function relativeTime(value: string) {
  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  if (elapsedSeconds < 60) return "Just now";
  const minutes = Math.floor(elapsedSeconds / 60);
  if (minutes < 60) return `${minutes} ${minutes === 1 ? "minute" : "minutes"} ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} ${hours === 1 ? "hour" : "hours"} ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Yesterday";
  if (days < 7) return `${days} days ago`;
  return new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(new Date(value));
}

function ChapterLink({ chapter }: Readonly<{ chapter: ChapterSummary }>) {
  return (
    <Link
      className="update-chapter"
      href={`/novels/${chapter.novelId}/chapters/${chapter.id}`}
      title={`Chapter ${chapter.chapterNumber}: ${chapter.title}`}
    >
      <span className="update-chapter__name">
        C{chapter.chapterNumber}: {readableTitle(chapter.title)}
      </span>
      <time dateTime={chapter.updatedAt}>{relativeTime(chapter.updatedAt)}</time>
    </Link>
  );
}

function FeaturedCarousel({ novels }: Readonly<{ novels: NovelSummary[] }>) {
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    if (novels.length < 2) return;
    const timer = window.setInterval(() => {
      setActiveIndex((index) => (index + 1) % novels.length);
    }, 6500);
    return () => window.clearInterval(timer);
  }, [novels.length]);

  if (!novels.length) return <p className="home-feedback">No novels are available yet.</p>;

  const novel = novels[activeIndex % novels.length];
  const moveSlide = (direction: -1 | 1) => {
    setActiveIndex((index) => (index + direction + novels.length) % novels.length);
  };

  return (
    <div className="featured-carousel" aria-roledescription="carousel" aria-label="Featured novels">
      <div className="featured-layout" key={novel.id}>
        <Link className="featured-cover-link" href={`/novels/${novel.id}`} aria-label={`Open ${readableTitle(novel.title)}`}>
          <NovelCover novel={novel} sizes="(max-width: 700px) 54vw, 252px" priority />
        </Link>
        <div className="featured-copy">
          <Link className="featured-title" href={`/novels/${novel.id}`}>
            {readableTitle(novel.title)}
          </Link>
          <div className="novel-meta" aria-label="Novel details">
            <span>{displayValue(novel.status)}</span>
            <span>{displayValue(novel.sourceLanguage)}</span>
          </div>
          <div
            className="featured-description"
            dangerouslySetInnerHTML={{
              __html: novel.description || "A new story is waiting to be discovered across the sea.",
            }}
          />
          <Link className="read-novel-link" href={`/novels/${novel.id}`}>Read Novel</Link>
        </div>
      </div>
      {novels.length > 1 && (
        <div className="featured-controls">
          <button className="carousel-arrow" type="button" onClick={() => moveSlide(-1)} aria-label="Previous featured novel">‹</button>
          <div className="carousel-dots" aria-label="Choose featured novel">
            {novels.map((item, index) => (
              <button
                key={item.id}
                className="carousel-indicator"
                type="button"
                aria-label={`Show ${readableTitle(item.title)}`}
                aria-current={index === activeIndex}
                onClick={() => setActiveIndex(index)}
              />
            ))}
          </div>
          <button className="carousel-arrow" type="button" onClick={() => moveSlide(1)} aria-label="Next featured novel">›</button>
        </div>
      )}
    </div>
  );
}

export default function SeaOfTalesHome() {
  const [homeData, setHomeData] = useState<PublicHomeData | null>(null);
  const [error, setError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/home", { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error("Home data could not be loaded.");
        return response.json() as Promise<PublicHomeData>;
      })
      .then(setHomeData)
      .catch((reason: unknown) => {
        if (reason instanceof Error && reason.name === "AbortError") return;
        setError(true);
      });
    return () => controller.abort();
  }, [reloadKey]);

  return (
    <div className="min-h-screen">
      <SiteNavbar />
      <main>
        {error && (
          <div className="home-feedback" role="alert">
            <p>Stories could not be reached right now.</p>
            <button
              type="button"
              onClick={() => {
                setError(false);
                setHomeData(null);
                setReloadKey((key) => key + 1);
              }}
            >
              Try again
            </button>
          </div>
        )}
        {!error && !homeData && (
          <output className="home-feedback">Finding stories at sea…</output>
        )}
        {!error && homeData && (
          <>
            <section id="featured" className="home-featured">
              <div className="home-section home-section--featured">
                <div className="mb-6">
                  <span className="home-eyebrow">Stories worth setting sail for</span>
                  <h1 className="text-[13px] font-bold uppercase tracking-[0.15em]">Featured Novels</h1>
                </div>
                <FeaturedCarousel novels={homeData.featuredNovels} />
              </div>
            </section>

            <section id="popular" className="home-popular">
              <div className="home-section home-section--popular">
                <div className="mb-6">
                  <span className="home-eyebrow">A place to begin</span>
                  <h2 className="text-[30px] font-semibold leading-tight">Popular Novels</h2>
                </div>
                <div className="popular-grid">
                  {homeData.popularNovels.map(({ novel, latestChapter }) => (
                    <article className="popular-item" key={novel.id}>
                      <Link className="popular-cover" href={`/novels/${novel.id}`}>
                        <NovelCover novel={novel} sizes="(max-width: 700px) 44vw, 250px" />
                      </Link>
                      <div className="popular-body">
                        <Link className="popular-title" href={`/novels/${novel.id}`}>
                          {readableTitle(novel.title)}
                        </Link>
                        {latestChapter ? (
                          <Link className="popular-chapter" href={`/novels/${novel.id}/chapters/${latestChapter.id}`}>
                            Latest · Chapter {latestChapter.chapterNumber}
                          </Link>
                        ) : (
                          <span className="popular-chapter">No chapters yet</span>
                        )}
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            </section>

            <section id="updates" className="home-updates">
              <div className="home-section home-section--updates">
                <div className="mb-5">
                  <span className="home-eyebrow">Fresh from the tide</span>
                  <h2 className="text-[30px] font-semibold leading-tight">Latest Updates</h2>
                </div>
                <div className="updates-grid">
                  {homeData.latestUpdates.map(({ novel, recentChapters }) => (
                    <article className="update-item" key={novel.id}>
                      <Link className="update-cover" href={`/novels/${novel.id}`} aria-label={`Open ${readableTitle(novel.title)}`}>
                        <NovelCover novel={novel} sizes="72px" />
                      </Link>
                      <div className="update-body">
                        <Link className="update-title" href={`/novels/${novel.id}`}>
                          {readableTitle(novel.title)}
                        </Link>
                        <div className="update-chapters">
                          {recentChapters.map((chapter) => <ChapterLink key={chapter.id} chapter={chapter} />)}
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
                <Link className="all-updates-link" href="/updates">
                  All Updates <span aria-hidden="true">→</span>
                </Link>
              </div>
            </section>
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}
