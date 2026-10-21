"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import type { NovelSummary } from "@/components/home/home-types";
import NovelCard from "@/components/catalog/NovelCard";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

type CatalogResponse = {
  content: NovelSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

const PAGE_SIZE = 20;
const languages = ["JAPANESE", "KOREAN"] as const;
const statuses = ["UPCOMING", "ONGOING", "DROPPED", "HIATUS", "COMPLETED"] as const;
const sortOptions = ["updated", "newest", "oldest", "title"] as const;

function humanize(value: string) {
  return value.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function CatalogContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const query = searchParams.get("q")?.trim() ?? "";
  const requestedLanguage = searchParams.get("language") ?? "";
  const language = languages.includes(requestedLanguage as (typeof languages)[number]) ? requestedLanguage : "";
  const requestedStatus = searchParams.get("status") ?? "";
  const status = statuses.includes(requestedStatus as (typeof statuses)[number]) ? requestedStatus : "";
  const requestedSort = searchParams.get("sort") ?? "updated";
  const sort = sortOptions.includes(requestedSort as (typeof sortOptions)[number]) ? requestedSort : "updated";
  const requestedPage = Number.parseInt(searchParams.get("page") ?? "0", 10);
  const page = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const apiParams = new URLSearchParams();
  if (query) apiParams.set("q", query);
  if (language) apiParams.set("language", language);
  if (status) apiParams.set("status", status);
  apiParams.set("sort", sort);
  apiParams.set("page", String(page));
  apiParams.set("size", String(PAGE_SIZE));
  const apiQuery = apiParams.toString();

  const [responseState, setResponseState] = useState<{
    requestKey: string;
    catalog: CatalogResponse;
  } | null>(null);
  const [failedRequestKey, setFailedRequestKey] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const requestKey = `${apiQuery}:${retryKey}`;

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/novels/catalog?${apiQuery}`, { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error("Catalog request failed.");
        return response.json() as Promise<CatalogResponse>;
      })
      .then((catalog) => setResponseState({ requestKey, catalog }))
      .catch((reason: unknown) => {
        if (reason instanceof Error && reason.name === "AbortError") return;
        setFailedRequestKey(requestKey);
      });
    return () => controller.abort();
  }, [apiQuery, requestKey]);

  const catalog = responseState?.requestKey === requestKey ? responseState.catalog : null;
  const loading = catalog === null && failedRequestKey !== requestKey;
  const error = catalog === null && failedRequestKey === requestKey;

  function updateQuery(changes: Partial<{ q: string; language: string; status: string; sort: string; page: number }>) {
    const next = {
      q: query,
      language,
      status,
      sort,
      page,
      ...changes,
    };
    const params = new URLSearchParams();
    if (next.q.trim()) params.set("q", next.q.trim());
    if (next.language) params.set("language", next.language);
    if (next.status) params.set("status", next.status);
    if (next.sort !== "updated") params.set("sort", next.sort);
    if (next.page > 0) params.set("page", String(next.page));
    const queryString = params.toString();
    router.push(queryString ? `/novels?${queryString}` : "/novels", { scroll: false });
  }

  const pageButtons = catalog
    ? Array.from({ length: catalog.totalPages }, (_, index) => index)
        .filter((index) => Math.abs(index - page) <= 2 || index === 0 || index === catalog.totalPages - 1)
    : [];

  return (
    <>
      <SiteNavbar />
      <main>
        <section className="home-section home-section--popular novels-section">
          <header className="novels-heading">
            <p className="home-eyebrow">Find your next story</p>
            <h1 className="text-3xl font-semibold leading-tight">Novels</h1>
          </header>

          <form
            className="novels-filters"
            role="search"
            onSubmit={(event) => {
              event.preventDefault();
              const formData = new FormData(event.currentTarget);
              updateQuery({ q: String(formData.get("q") ?? ""), page: 0 });
            }}
          >
            <div className="novels-search">
              <label htmlFor="novels-query">Search</label>
              <span className="novels-search__control">
                <input id="novels-query" key={query} name="q" type="search" defaultValue={query} placeholder="Search novels..." />
                <button type="submit">Search</button>
              </span>
            </div>
            <div className="novels-filter-row">
              <label>
                <span>Sort</span>
                <select value={sort} onChange={(event) => updateQuery({ sort: event.target.value, page: 0 })}>
                  <option value="updated">Recently Updated</option>
                  <option value="newest">Newest</option>
                  <option value="oldest">Oldest</option>
                  <option value="title">Title A–Z</option>
                </select>
              </label>
              <label>
                <span>Language</span>
                <select value={language} onChange={(event) => updateQuery({ language: event.target.value, page: 0 })}>
                  <option value="">All Languages</option>
                  {languages.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
                </select>
              </label>
              <label>
                <span>Status</span>
                <select value={status} onChange={(event) => updateQuery({ status: event.target.value, page: 0 })}>
                  <option value="">All Statuses</option>
                  {statuses.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
                </select>
              </label>
            </div>
          </form>

          {loading && !catalog && <p className="novels-feedback" role="status">Finding stories at sea…</p>}
          {loading && catalog && <p className="novels-feedback" role="status">Updating the catalog…</p>}
          {error && (
            <div className="novels-feedback" role="alert">
              <p>The catalog could not be reached right now.</p>
              <button type="button" onClick={() => setRetryKey((key) => key + 1)}>Try again</button>
            </div>
          )}
          {!loading && !error && catalog?.content.length === 0 && (
            <div className="novels-empty">
              <h2>No novels found.</h2>
              <p>Try changing your search or filters.</p>
            </div>
          )}
          {!error && catalog && catalog.content.length > 0 && (
            <>
              <p className="novels-result-count" aria-live="polite">
                {catalog.totalElements} {catalog.totalElements === 1 ? "novel" : "novels"}
              </p>
              <div className="novels-grid grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {catalog.content.map((novel, index) => (
                  <NovelCard key={novel.id} novel={novel} priority={index === 0} />
                ))}
              </div>
              {catalog.totalPages > 1 && (
                <nav className="novels-pagination" aria-label="Catalog pages">
                  <button type="button" disabled={page === 0} onClick={() => updateQuery({ page: page - 1 })}>Previous</button>
                  {pageButtons.map((index, position) => (
                    <span className="novels-pagination__group" key={index}>
                      {position > 0 && index - pageButtons[position - 1] > 1 && <span aria-hidden="true">…</span>}
                      <button
                        type="button"
                        aria-current={index === page ? "page" : undefined}
                        onClick={() => updateQuery({ page: index })}
                      >
                        {index + 1}
                      </button>
                    </span>
                  ))}
                  <button type="button" disabled={page >= catalog.totalPages - 1} onClick={() => updateQuery({ page: page + 1 })}>Next</button>
                </nav>
              )}
            </>
          )}
        </section>
      </main>
      <SiteFooter />
    </>
  );
}

export default function NovelCatalogPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen">
          <SiteNavbar />
          <main>
            <section className="home-section home-section--popular novels-section">
              <p className="home-eyebrow">Find your next story</p>
              <h1 className="text-3xl font-semibold leading-tight">Novels</h1>
              <p className="novels-feedback" role="status">Loading the catalog…</p>
            </section>
          </main>
          <SiteFooter />
        </div>
      }
    >
      <CatalogContent />
    </Suspense>
  );
}
