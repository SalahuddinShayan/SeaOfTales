"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

interface Novel {
    id: number;
    title: string;
    authorUsername: string;
    sourceLanguage: string;
    status: string;
    coverImage: string | null;
}

export default function AdminNovelsPage() {
    const [novels, setNovels] = useState<Novel[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadNovels() {
            try {
                const response = await fetch("/api/novels");
                if (!response.ok) {
                    throw new Error("Failed to load novels.");
                }
                setNovels(await response.json());
            } catch (loadError) {
                setError(loadError instanceof Error ? loadError.message : "Failed to load novels.");
            } finally {
                setLoading(false);
            }
        }

        loadNovels();
    }, []);

    return (
        <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white">
            <div className="absolute inset-0 bg-cover bg-center opacity-25" style={{ backgroundImage: "url('/images/sea-night.png')" }} aria-hidden="true" />
            <div className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]" aria-hidden="true" />

            <div className="relative mx-auto max-w-7xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12">
                <nav className="flex items-center justify-between gap-4">
                    <Link href="/" className="text-sm font-semibold tracking-[0.2em] text-white transition-opacity hover:opacity-75 sm:text-base">SEA OF TALES</Link>
                    <Link href="/novels/new" className="rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50">Create New Novel</Link>
                </nav>

                <header className="mt-12">
                    <p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">CONTENT MANAGEMENT</p>
                    <h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Novel administration</h1>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-white/70 sm:text-base">Manage the source novels that feed the chapter, terminology, and prompt-instruction workflows.</p>
                </header>

                <section className="mt-8 overflow-hidden rounded-3xl border border-white/20 bg-black/25 shadow-2xl shadow-black/30 backdrop-blur-md">
                    {loading && <p className="p-6 text-white/75">Loading novels...</p>}
                    {error && <p className="m-6 rounded-xl border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100">{error}</p>}
                    {!loading && !error && novels.length === 0 && (
                        <div className="p-8 text-center sm:p-12">
                            <p className="text-lg font-medium">No novels yet</p>
                            <p className="mt-2 text-sm text-white/65">Create the first source novel to begin building its translation workspace.</p>
                            <Link href="/novels/new" className="mt-5 inline-block rounded-full bg-white px-5 py-2.5 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50">Create New Novel</Link>
                        </div>
                    )}
                    {!loading && !error && novels.length > 0 && (
                        <div className="divide-y divide-white/15">
                            {novels.map((novel) => (
                                <article key={novel.id} className="flex flex-col gap-5 p-6 sm:p-8 lg:flex-row lg:items-center">
                                    <div className="flex min-w-0 flex-1 items-center gap-4">
                                        <div className="flex h-16 w-12 shrink-0 items-center justify-center rounded-lg border border-white/20 bg-white/10 text-xs font-semibold tracking-widest text-white/70" aria-label={novel.coverImage ? "Cover image uploaded" : "No cover image"}>
                                            {novel.coverImage ? "COVER" : "—"}
                                        </div>
                                        <div className="min-w-0">
                                            <h2 className="truncate text-lg font-semibold">{novel.title}</h2>
                                            <p className="mt-1 text-sm text-white/65">{novel.authorUsername} · {novel.sourceLanguage}</p>
                                            <p className="mt-2 inline-flex rounded-full border border-white/20 bg-white/10 px-2.5 py-1 text-xs font-medium tracking-wide text-white/85">{novel.status}</p>
                                        </div>
                                    </div>
                                    <div className="flex flex-wrap gap-2 lg:justify-end">
                                        <Link href={`/admin/novels/${novel.id}/chapters`} className="rounded-full border border-white/25 px-3 py-2 text-sm text-white/90 transition hover:bg-white/15">Chapters</Link>
                                        <Link href={`/admin/novels/${novel.id}/terminology`} className="rounded-full border border-white/25 px-3 py-2 text-sm text-white/90 transition hover:bg-white/15">Terminology</Link>
                                        <Link href={`/admin/novels/${novel.id}/prompt-instructions`} className="rounded-full border border-white/25 px-3 py-2 text-sm text-white/90 transition hover:bg-white/15">Prompt Instructions</Link>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                </section>
                <p className="mt-4 text-xs text-white/50">Chapter, terminology, and prompt-instruction management will be available at their linked novel-specific routes as those workflows are implemented.</p>
            </div>
        </main>
    );
}
