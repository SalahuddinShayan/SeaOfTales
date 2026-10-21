"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

interface Novel { id: number; title: string; authorUsername: string; sourceLanguage: string; }
interface Chapter { id: number; chapterNumber: string; title: string; state: string; translationExists: boolean; }

export default function ChapterListPage() {
    const params = useParams<{ novelId: string }>();
    const router = useRouter();
    const novelId = Number(params.novelId);
    const [novel, setNovel] = useState<Novel | null>(null);
    const [chapters, setChapters] = useState<Chapter[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [buildingId, setBuildingId] = useState<number | null>(null);

    useEffect(() => {
        async function loadData() {
            try {
                const [novelsResponse, chaptersResponse] = await Promise.all([fetch("/api/novels"), fetch(`/api/novels/${novelId}/chapters`)]);
                if (!novelsResponse.ok || !chaptersResponse.ok) throw new Error("Failed to load chapter data.");
                const novels: Novel[] = await novelsResponse.json();
                setNovel(novels.find((item) => item.id === novelId) ?? null);
                setChapters(await chaptersResponse.json());
            } catch (loadError) {
                setError(loadError instanceof Error ? loadError.message : "Failed to load chapter data.");
            } finally {
                setLoading(false);
            }
        }
        loadData();
    }, [novelId]);

    async function buildPrompt(chapterId: number) {
        setBuildingId(chapterId);
        setError("");
        try {
            const response = await fetch(`/api/chapters/${chapterId}/prompt`, { method: "POST" });
            const data = await response.json().catch(() => null);
            if (!response.ok) throw new Error(data?.message || "Failed to build prompt.");
            router.push(`/admin/novels/${novelId}/chapters/${chapterId}/prompt`);
        } catch (buildError) {
            setError(buildError instanceof Error ? buildError.message : "Failed to build prompt.");
        } finally {
            setBuildingId(null);
        }
    }

    return (
        <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white"><div className="absolute inset-0 bg-cover bg-center opacity-25" style={{ backgroundImage: "url('/images/sea-night.png')" }} aria-hidden="true" /><div className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]" aria-hidden="true" /><div className="relative mx-auto max-w-7xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12"><nav className="flex items-center justify-between gap-4"><Link href="/" className="text-sm font-semibold tracking-[0.2em] text-white sm:text-base">SEA OF TALES</Link><Link href="/admin/novels" className="rounded-full border border-white/35 px-4 py-2 text-sm text-white hover:bg-white/15">Novel Admin</Link></nav><header className="mt-12 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">CONTENT MANAGEMENT</p><h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Chapters</h1><p className="mt-3 text-sm text-white/70">{novel ? `${novel.title} · ${novel.sourceLanguage}` : "Manage uploaded source chapters."}</p></div><Link href={`/admin/novels/${novelId}/chapters/new`} className="w-fit rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#07141c] hover:bg-cyan-50">Add Chapter</Link></header><section className="mt-8 overflow-hidden rounded-3xl border border-white/20 bg-black/25 shadow-2xl shadow-black/30 backdrop-blur-md">{loading && <p className="p-6 text-white/70">Loading chapters...</p>}{error && <p className="m-6 rounded-xl border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100">{error}</p>}{!loading && !error && chapters.length === 0 && <div className="p-8 text-center"><p className="text-lg font-medium">No chapters yet</p><p className="mt-2 text-sm text-white/65">Upload the first original source chapter for this novel.</p></div>}{!loading && !error && chapters.length > 0 && <div className="divide-y divide-white/15">{chapters.map((chapter) => <article key={chapter.id} className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between sm:p-8"><div><p className="text-xs font-semibold tracking-[0.18em] text-cyan-100/70">CHAPTER {chapter.chapterNumber}</p><h2 className="mt-2 text-lg font-semibold">{chapter.title}</h2><p className="mt-2 inline-flex rounded-full border border-white/20 bg-white/10 px-2.5 py-1 text-xs text-white/80">{chapter.state}</p></div><button type="button" onClick={() => buildPrompt(chapter.id)} disabled={buildingId !== null} className="w-fit rounded-full border border-white/25 px-4 py-2 text-sm text-white/90 transition hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-50">{buildingId === chapter.id ? "Building..." : "Build Prompt"}</button></article>)}</div>}</section></div></main>
    );
}
