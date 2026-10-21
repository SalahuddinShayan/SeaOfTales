"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

interface Novel {
    id: number;
    title: string;
    authorUsername: string;
    sourceLanguage: string;
}

interface CreatedChapter {
    chapterId: number;
    novelId: number;
    chapterNumber: string;
    title: string;
    state: string;
}

export default function AddChapterPage() {
    const params = useParams<{ novelId?: string }>();
    const routeNovelId = params.novelId ? Number(params.novelId) : null;
    const hasRouteNovel = routeNovelId !== null && Number.isInteger(routeNovelId);

    const [novels, setNovels] = useState<Novel[]>([]);
    const [novelId, setNovelId] = useState(hasRouteNovel ? String(routeNovelId) : "");
    const [chapterNumber, setChapterNumber] = useState("");
    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [createdChapter, setCreatedChapter] = useState<CreatedChapter | null>(null);

    const selectedNovel = useMemo(() => novels.find((novel) => String(novel.id) === novelId), [novels, novelId]);

    useEffect(() => {
        async function loadNovels() {
            try {
                const response = await fetch("/api/novels");
                if (!response.ok) throw new Error("Failed to load novels.");
                setNovels(await response.json());
            } catch (loadError) {
                setError(loadError instanceof Error ? loadError.message : "Failed to load novels.");
            } finally {
                setLoading(false);
            }
        }

        loadNovels();
    }, []);

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setSubmitting(true);
        setMessage("");
        setError("");
        setCreatedChapter(null);

        try {
            const response = await fetch("/api/chapters", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    novelId: Number(novelId),
                    chapterNumber,
                    title,
                    content,
                }),
            });
            const responseText = await response.text();
            const responseData = responseText ? JSON.parse(responseText) : null;
            if (!response.ok) {
                throw new Error(responseData?.message || "Failed to ingest chapter source.");
            }

            setCreatedChapter(responseData);
            setMessage("Chapter source ingested successfully.");
            setChapterNumber("");
            setTitle("");
            setContent("");
        } catch (submitError) {
            setError(submitError instanceof Error ? submitError.message : "Failed to ingest chapter source.");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white">
            <div className="absolute inset-0 bg-cover bg-center opacity-25" style={{ backgroundImage: "url('/images/sea-night.png')" }} aria-hidden="true" />
            <div className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]" aria-hidden="true" />
            <div className="relative mx-auto max-w-5xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12">
                <nav className="flex items-center justify-between gap-4">
                    <Link href="/" className="text-sm font-semibold tracking-[0.2em] text-white transition-opacity hover:opacity-75 sm:text-base">SEA OF TALES</Link>
                    <Link href="/admin/novels" className="rounded-full border border-white/35 bg-black/15 px-4 py-2 text-sm text-white backdrop-blur-sm transition hover:bg-white/15">Novel Admin</Link>
                </nav>

                <header className="mt-12">
                    <p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">SOURCE INGESTION</p>
                    <h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Add chapter</h1>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-white/70 sm:text-base">Store the original chapter source exactly as submitted. Translation processing happens later.</p>
                </header>

                {selectedNovel && <div className="mt-8 rounded-2xl border border-cyan-100/35 bg-cyan-950/30 px-5 py-4 shadow-lg shadow-black/20"><p className="text-xs font-semibold tracking-[0.18em] text-cyan-100/75">SELECTED NOVEL</p><p className="mt-1 text-lg font-medium">{selectedNovel.title}</p><p className="mt-1 text-sm text-white/65">{selectedNovel.authorUsername} · {selectedNovel.sourceLanguage} · ID {selectedNovel.id}</p></div>}

                <section className="mt-8 rounded-3xl border border-white/20 bg-black/25 p-6 shadow-2xl shadow-black/30 backdrop-blur-md sm:p-10">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div><label htmlFor="novelId" className="mb-2 block text-sm font-medium text-white/90">Novel</label><select id="novelId" value={novelId} onChange={(event) => setNovelId(event.target.value)} required disabled={hasRouteNovel} className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-60 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="" disabled>Select a novel</option>{novels.map((novel) => <option key={novel.id} value={novel.id}>{novel.title}</option>)}</select></div>
                        <div className="grid gap-6 sm:grid-cols-2"><div><label htmlFor="chapterNumber" className="mb-2 block text-sm font-medium text-white/90">Chapter Number</label><input id="chapterNumber" value={chapterNumber} onChange={(event) => setChapterNumber(event.target.value)} required placeholder="10.5 or Prologue" className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" /></div><div><label htmlFor="title" className="mb-2 block text-sm font-medium text-white/90">Chapter Title</label><input id="title" value={title} onChange={(event) => setTitle(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" /></div></div>
                        <div><label htmlFor="content" className="mb-2 block text-sm font-medium text-white/90">Original Source Text</label><textarea id="content" value={content} onChange={(event) => setContent(event.target.value)} required rows={20} spellCheck={false} className="w-full resize-y rounded-xl border border-white/20 bg-black/25 px-4 py-3 font-mono text-sm leading-6 text-white outline-none placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" placeholder="Paste the original source chapter here..." /></div>
                        <button type="submit" disabled={submitting || loading} className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-60">{submitting ? "Saving..." : "Ingest Chapter Source"}</button>
                    </form>
                    {message && <p className="mt-6 rounded-xl border border-emerald-200/25 bg-emerald-950/30 px-4 py-3 text-sm text-emerald-100" role="status">{message}</p>}
                    {error && <p className="mt-6 rounded-xl border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100" role="alert">{error}</p>}
                    {createdChapter && <div className="mt-6 rounded-xl border border-cyan-100/25 bg-cyan-950/25 px-4 py-4 text-sm text-cyan-50"><p className="font-semibold">Chapter {createdChapter.chapterNumber} · {createdChapter.title}</p><p className="mt-1 text-cyan-100/75">Chapter ID {createdChapter.chapterId} · State {createdChapter.state}</p></div>}
                </section>
            </div>
        </main>
    );
}
