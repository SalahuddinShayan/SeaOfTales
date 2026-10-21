"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import NovelCard from "@/components/catalog/NovelCard";
import type { NovelSummary } from "@/components/home/home-types";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

type Author = {
  username: string;
  profilePicture: string | null;
  publicNovelCount: number;
  novels: NovelSummary[];
};

function authorUrl(username: string) {
  return `/authors/${encodeURIComponent(username)}`;
}

function AuthorPortrait({ author, large = false }: { author: Author; large?: boolean }) {
  const size = large ? "h-32 w-32" : "h-24 w-24";
  return author.profilePicture ? (
    <Image
      src={author.profilePicture}
      alt={`${author.username} profile picture`}
      width={large ? 128 : 96}
      height={large ? 128 : 96}
      unoptimized
      className={`${size} shrink-0 rounded-full border border-[var(--page-border)] object-cover`}
    />
  ) : (
    <div aria-hidden="true" className={`${size} grid shrink-0 place-items-center rounded-full border border-[var(--page-border)] bg-[var(--footer-background)] text-3xl font-semibold text-[var(--page-accent)]`}>
      {author.username.slice(0, 1).toUpperCase()}
    </div>
  );
}

function AuthorCard({ author }: { author: Author }) {
  return (
    <article className="rounded-lg border border-[var(--page-border)] bg-[var(--page-card)] p-5 text-[var(--page-ink)] sm:p-6">
      <header className="flex flex-col gap-5 sm:flex-row sm:items-center">
        <AuthorPortrait author={author} />
        <div className="min-w-0 flex-1">
          <p className="home-eyebrow">Author</p>
          <h2 className="text-2xl font-semibold">{author.username}</h2>
          <p className="mt-2 text-sm text-[var(--page-muted)]">
            {author.publicNovelCount} {author.publicNovelCount === 1 ? "public novel" : "public novels"}
          </p>
          <Link href={authorUrl(author.username)} className="mt-3 inline-block text-sm font-medium text-[var(--page-accent)] underline underline-offset-4">
            View author profile and all novels
          </Link>
        </div>
      </header>
      {author.novels.length > 0 ? (
        <section className="mt-6 border-t border-[var(--page-border)] pt-5" aria-label={`${author.username} novel previews`}>
          <h3 className="mb-4 text-sm font-semibold text-[var(--page-muted)]">Featured novels</h3>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
            {author.novels.slice(0, 3).map((novel) => <NovelCard key={novel.id} novel={novel} />)}
          </div>
        </section>
      ) : (
        <p className="mt-6 border-t border-[var(--page-border)] pt-5 text-sm text-[var(--page-muted)]">
          This author has no publicly available novels yet.
        </p>
      )}
    </article>
  );
}

export function AuthorsDirectoryPage() {
  const [authors, setAuthors] = useState<Author[] | null>(null);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/authors", { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error("Could not load authors.");
        return response.json() as Promise<Author[]>;
      })
      .then(setAuthors)
      .catch((reason: unknown) => {
        if (reason instanceof Error && reason.name === "AbortError") return;
        setError(true);
      });
    return () => controller.abort();
  }, [retry]);

  return (
    <div className="min-h-screen">
      <SiteNavbar />
      <main className="home-section home-section--popular min-h-[58vh]">
        <header className="novels-heading">
          <p className="home-eyebrow">Meet the storytellers</p>
          <h1 className="text-3xl font-semibold leading-tight">Authors</h1>
          <p className="mt-3 max-w-2xl text-sm text-[var(--page-muted)]">Discover the writers behind the stories and explore a selection of their work.</p>
        </header>
        {error ? (
          <div className="novels-feedback" role="alert">
            <p>The author directory could not be loaded.</p>
            <button type="button" onClick={() => { setError(false); setAuthors(null); setRetry((value) => value + 1); }}>Try again</button>
          </div>
        ) : authors === null ? (
          <p className="novels-feedback" role="status">Loading authors…</p>
        ) : authors.length === 0 ? (
          <p className="novels-feedback">There are no authors in the directory yet.</p>
        ) : (
          <div className="grid grid-cols-1 gap-6">
            {authors.map((author) => <AuthorCard key={author.username} author={author} />)}
          </div>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}

export function AuthorProfilePage() {
  const params = useParams<{ slug: string }>();
  const slug = params.slug;
  const [retry, setRetry] = useState(0);
  const requestKey = `${slug}:${retry}`;
  const [result, setResult] = useState<{
    requestKey: string;
    state: "loaded" | "missing" | "error";
    author?: Author;
  } | null>(null);
  const state = result?.requestKey === requestKey ? result.state : "loading";
  const author = result?.requestKey === requestKey ? result.author ?? null : null;

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/authors/${encodeURIComponent(slug)}`, { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (response.status === 404) { setResult({ requestKey, state: "missing" }); return null; }
        if (!response.ok) throw new Error("Could not load author.");
        return response.json() as Promise<Author>;
      })
      .then((data) => {
        if (!data) return;
        setResult({ requestKey, state: "loaded", author: data });
      })
      .catch((reason: unknown) => {
        if (reason instanceof Error && reason.name === "AbortError") return;
        setResult({ requestKey, state: "error" });
      });
    return () => controller.abort();
  }, [slug, requestKey]);

  return (
    <div className="min-h-screen">
      <SiteNavbar />
      <main className="home-section home-section--popular min-h-[58vh]">
        {state === "loading" && <p className="novels-feedback" role="status">Loading author…</p>}
        {state === "missing" && <section className="novels-feedback" role="status"><h1 className="text-3xl font-semibold text-[var(--page-ink)]">Author not found</h1><p className="mt-3">This author page does not exist.</p><Link className="mt-5 inline-block underline" href="/authors">Browse authors</Link></section>}
        {state === "error" && <section className="novels-feedback" role="alert"><p>The author page could not be loaded.</p><button type="button" onClick={() => setRetry((value) => value + 1)}>Try again</button></section>}
        {state === "loaded" && author && (
          <>
            <header className="mb-8 flex flex-col gap-5 border-b border-[var(--page-border)] pb-8 sm:flex-row sm:items-center">
              <AuthorPortrait author={author} large />
              <div>
                <p className="home-eyebrow">Author profile</p>
                <h1 className="text-3xl font-semibold leading-tight">{author.username}</h1>
                <p className="mt-2 text-sm text-[var(--page-muted)]">
                  {author.publicNovelCount} {author.publicNovelCount === 1 ? "public novel" : "public novels"}
                </p>
              </div>
            </header>
            <section aria-labelledby="author-novels-heading">
              <h2 id="author-novels-heading" className="mb-5 text-2xl font-semibold">Novels</h2>
              {author.novels.length === 0 ? (
                <p className="novels-feedback">This author has no publicly available novels yet.</p>
              ) : (
                <div className="novels-grid grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                  {author.novels.map((novel) => <NovelCard key={novel.id} novel={novel} />)}
                </div>
              )}
            </section>
            <Link className="mt-8 inline-block text-sm underline underline-offset-4" href="/authors">All authors</Link>
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}
