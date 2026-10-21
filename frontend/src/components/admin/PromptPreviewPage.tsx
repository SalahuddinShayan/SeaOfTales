"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

interface PromptResponse { chapterId: number; novelId: number; chapterNumber: string; title: string; state: string; prompt: string; }

export default function PromptPreviewPage() {
    const params = useParams<{ novelId: string; chapterId: string }>();
    const [data, setData] = useState<PromptResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        async function loadPrompt() {
            try {
                const response = await fetch(`/api/chapters/${params.chapterId}/prompt`, { method: "POST" });
                const responseData = await response.json().catch(() => null);
                if (!response.ok) throw new Error(responseData?.message || "Failed to build prompt.");
                setData(responseData);
            } catch (loadError) {
                setError(loadError instanceof Error ? loadError.message : "Failed to build prompt.");
            } finally {
                setLoading(false);
            }
        }
        loadPrompt();
    }, [params.chapterId]);

    async function copyPrompt() {
        if (!data) return;
        try {
            await navigator.clipboard.writeText(data.prompt);
            setCopied(true);
            window.setTimeout(() => setCopied(false), 1800);
        } catch {
            setError("Unable to copy the prompt. Please copy it manually.");
        }
    }

    return <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white"><div className="absolute inset-0 bg-cover bg-center opacity-25" style={{ backgroundImage: "url('/images/sea-night.png')" }} aria-hidden="true" /><div className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]" aria-hidden="true" /><div className="relative mx-auto max-w-7xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12"><nav className="flex items-center justify-between gap-4"><Link href="/" className="text-sm font-semibold tracking-[0.2em] text-white sm:text-base">SEA OF TALES</Link><Link href={`/admin/novels/${params.novelId}/chapters`} className="rounded-full border border-white/35 px-4 py-2 text-sm text-white hover:bg-white/15">Back to Chapters</Link></nav><header className="mt-12"><p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">PROMPT PREVIEW</p><h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Generated translation prompt</h1>{data && <p className="mt-3 text-sm text-white/70">Chapter {data.chapterNumber} · {data.title} · State {data.state}</p>}</header>{loading && <p className="mt-8 text-white/70">Building prompt...</p>}{error && <p className="mt-8 rounded-xl border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100">{error}</p>}{data && <section className="mt-8 rounded-3xl border border-white/20 bg-black/25 p-6 shadow-2xl shadow-black/30 backdrop-blur-md sm:p-8"><div className="flex flex-wrap items-center justify-between gap-4"><div><h2 className="text-xl font-semibold">Complete prompt</h2><p className="mt-1 text-sm text-white/60">Chapter ID {data.chapterId}</p></div><div className="flex gap-2"><Link href={`/admin/novels/${params.novelId}/chapters/${params.chapterId}/translation`} className="rounded-full border border-white/25 px-4 py-2 text-sm text-white hover:bg-white/15">Submit Translation</Link><button type="button" onClick={copyPrompt} className="rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#07141c] hover:bg-cyan-50">{copied ? "Copied!" : "Copy Prompt"}</button></div></div><textarea readOnly value={data.prompt} className="mt-6 min-h-[32rem] w-full resize-y rounded-xl border border-white/20 bg-black/35 px-4 py-3 font-mono text-sm leading-6 text-white/90 outline-none" aria-label="Generated prompt" /></section>}</div></main>;
}
