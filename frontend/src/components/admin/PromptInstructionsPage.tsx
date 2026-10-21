"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

interface Novel {
    id: number;
    title: string;
    authorUsername: string;
    sourceLanguage: string;
    status: string;
}

interface PromptInstruction {
    id: number;
    scope: string;
    language: string | null;
    novelId: number | null;
    instruction: string;
    priority: number;
    enabled: boolean;
}

interface NovelOptions {
    sourceLanguages: string[];
}

const scopes = ["GLOBAL", "LANGUAGE", "NOVEL"];

export default function PromptInstructionsPage() {
    const params = useParams<{ novelId?: string }>();
    const routeNovelId = params.novelId ? Number(params.novelId) : null;
    const hasRouteNovel = routeNovelId !== null && Number.isInteger(routeNovelId);

    const [novels, setNovels] = useState<Novel[]>([]);
    const [languages, setLanguages] = useState<string[]>([]);
    const [instructions, setInstructions] = useState<PromptInstruction[]>([]);
    const [scope, setScope] = useState(hasRouteNovel ? "NOVEL" : "");
    const [language, setLanguage] = useState("");
    const [novelId, setNovelId] = useState(hasRouteNovel ? String(routeNovelId) : "");
    const [instruction, setInstruction] = useState("");
    const [priority, setPriority] = useState("0");
    const [enabled, setEnabled] = useState(true);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const selectedNovel = useMemo(
        () => novels.find((novel) => String(novel.id) === novelId),
        [novels, novelId]
    );

    async function loadData() {
        try {
            const [novelsResponse, optionsResponse, instructionsResponse] = await Promise.all([
                fetch("/api/novels"),
                fetch("/api/novels/options"),
                fetch("/api/prompt-instructions"),
            ]);

            if (!novelsResponse.ok || !optionsResponse.ok || !instructionsResponse.ok) {
                throw new Error("Failed to load prompt instruction data.");
            }

            const novelData: Novel[] = await novelsResponse.json();
            const optionData: NovelOptions = await optionsResponse.json();
            setNovels(novelData);
            setLanguages(optionData.sourceLanguages);
            setInstructions(await instructionsResponse.json());
        } catch (loadError) {
            setError(loadError instanceof Error ? loadError.message : "Failed to load prompt instruction data.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        // Loading remote data is the synchronization this effect is responsible for.
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadData();
    }, []);

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setSubmitting(true);
        setMessage("");
        setError("");

        const body = {
            scope,
            language: scope === "LANGUAGE" || scope === "NOVEL" ? language || null : null,
            novelId: scope === "NOVEL" ? Number(novelId) : null,
            instruction,
            priority: Number(priority),
            enabled,
        };

        try {
            const response = await fetch("/api/prompt-instructions", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body),
            });
            const responseText = await response.text();
            const responseData = responseText ? JSON.parse(responseText) : null;

            if (!response.ok) {
                throw new Error(responseData?.message || "Failed to create prompt instruction.");
            }

            setMessage(`Prompt instruction created successfully. ID: ${responseData.id}`);
            setInstruction("");
            setPriority("0");
            setEnabled(true);
            if (!hasRouteNovel) {
                setScope("");
                setLanguage("");
                setNovelId("");
            }
            await loadData();
        } catch (submitError) {
            setError(submitError instanceof Error ? submitError.message : "Failed to create prompt instruction.");
        } finally {
            setSubmitting(false);
        }
    }

    function handleScopeChange(value: string) {
        setScope(value);
        if (value === "GLOBAL") {
            setLanguage("");
            if (!hasRouteNovel) setNovelId("");
        }
        if (value === "LANGUAGE" && !hasRouteNovel) setNovelId("");
        if (value === "NOVEL" && hasRouteNovel) setNovelId(String(routeNovelId));
    }

    return (
        <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white">
            <div className="absolute inset-0 bg-cover bg-center opacity-25" style={{ backgroundImage: "url('/images/sea-night.png')" }} aria-hidden="true" />
            <div className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]" aria-hidden="true" />

            <div className="relative mx-auto max-w-7xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12">
                <nav className="flex items-center justify-between gap-4">
                    <Link href="/" className="text-sm font-semibold tracking-[0.2em] text-white transition-opacity hover:opacity-75 sm:text-base">SEA OF TALES</Link>
                    <Link href="/admin/novels" className="rounded-full border border-white/35 bg-black/15 px-4 py-2 text-sm text-white backdrop-blur-sm transition hover:bg-white/15">Novel Admin</Link>
                </nav>

                <header className="mt-12">
                    <p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">TRANSLATION WORKSPACE</p>
                    <h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Prompt instructions</h1>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-white/70 sm:text-base">Create the guidance that will shape future translation work across the whole platform, a language, or one novel.</p>
                </header>

                {selectedNovel && (
                    <div className="mt-8 rounded-2xl border border-cyan-100/35 bg-cyan-950/30 px-5 py-4 shadow-lg shadow-black/20">
                        <p className="text-xs font-semibold tracking-[0.18em] text-cyan-100/75">SELECTED NOVEL</p>
                        <p className="mt-1 text-lg font-medium">{selectedNovel.title}</p>
                        <p className="mt-1 text-sm text-white/65">{selectedNovel.authorUsername} · {selectedNovel.sourceLanguage} · ID {selectedNovel.id}</p>
                    </div>
                )}

                <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
                    <section className="rounded-3xl border border-white/20 bg-black/25 p-6 shadow-2xl shadow-black/30 backdrop-blur-md sm:p-8">
                        <h2 className="text-xl font-semibold">Add instruction</h2>
                        <p className="mt-2 text-sm text-white/65">A new instruction starts enabled with priority 0.</p>

                        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
                            <div>
                                <label htmlFor="scope" className="mb-2 block text-sm font-medium text-white/90">Scope</label>
                                <select id="scope" value={scope} onChange={(event) => handleScopeChange(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20">
                                    <option value="" disabled>Select a scope</option>
                                    {scopes.map((value) => <option key={value} value={value}>{value}</option>)}
                                </select>
                            </div>

                            <div>
                                <label htmlFor="language" className="mb-2 block text-sm font-medium text-white/90">Language{scope === "LANGUAGE" ? " *" : ""}</label>
                                <select id="language" value={language} onChange={(event) => setLanguage(event.target.value)} required={scope === "LANGUAGE"} disabled={scope !== "LANGUAGE" && scope !== "NOVEL"} className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-45 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20">
                                    <option value="">{scope === "LANGUAGE" ? "Select a language" : "Not used for this scope"}</option>
                                    {languages.map((value) => <option key={value} value={value}>{value}</option>)}
                                </select>
                            </div>

                            <div>
                                <label htmlFor="novelId" className="mb-2 block text-sm font-medium text-white/90">Novel{scope === "NOVEL" ? " *" : ""}</label>
                                <select id="novelId" value={novelId} onChange={(event) => setNovelId(event.target.value)} required={scope === "NOVEL"} disabled={scope !== "NOVEL" || hasRouteNovel} className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-45 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20">
                                    <option value="">{scope === "NOVEL" ? "Select a novel" : "Not used for this scope"}</option>
                                    {novels.map((novel) => <option key={novel.id} value={novel.id}>{novel.title}</option>)}
                                </select>
                            </div>

                            <div>
                                <label htmlFor="instruction" className="mb-2 block text-sm font-medium text-white/90">Instruction</label>
                                <textarea id="instruction" value={instruction} onChange={(event) => setInstruction(event.target.value)} required rows={8} className="w-full resize-y rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" placeholder="Describe how the translation should handle this context..." />
                            </div>

                            <div className="grid gap-5 sm:grid-cols-2">
                                <div>
                                    <label htmlFor="priority" className="mb-2 block text-sm font-medium text-white/90">Priority</label>
                                    <input id="priority" type="number" min="0" value={priority} onChange={(event) => setPriority(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" />
                                </div>
                                <label className="flex items-center gap-3 self-end pb-3 text-sm text-white/90">
                                    <input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} className="h-4 w-4 accent-cyan-200" />
                                    Enabled
                                </label>
                            </div>

                            <button type="submit" disabled={submitting || loading} className="w-full rounded-full bg-white px-6 py-3 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-60">{submitting ? "Saving..." : "Create Prompt Instruction"}</button>
                        </form>

                        {message && <p className="mt-5 rounded-xl border border-emerald-200/25 bg-emerald-950/30 px-4 py-3 text-sm text-emerald-100" role="status">{message}</p>}
                        {error && <p className="mt-5 rounded-xl border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100" role="alert">{error}</p>}
                    </section>

                    <section className="rounded-3xl border border-white/20 bg-black/25 shadow-2xl shadow-black/30 backdrop-blur-md">
                        <div className="border-b border-white/15 p-6 sm:p-8">
                            <h2 className="text-xl font-semibold">Existing instructions</h2>
                            <p className="mt-2 text-sm text-white/65">{loading ? "Loading..." : `${instructions.length} instruction${instructions.length === 1 ? "" : "s"} stored`}</p>
                        </div>
                        {!loading && instructions.length === 0 && <p className="p-6 text-sm text-white/65 sm:p-8">No prompt instructions have been created yet.</p>}
                        {!loading && instructions.length > 0 && (
                            <div className="divide-y divide-white/15">
                                {instructions.map((item) => (
                                    <article key={item.id} className="p-6 sm:p-8">
                                        <div className="flex flex-wrap items-center gap-2 text-xs font-medium tracking-wide text-white/80">
                                            <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">{item.scope}</span>
                                            {item.language && <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">{item.language}</span>}
                                            <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">Priority {item.priority}</span>
                                            <span className={`rounded-full border px-2.5 py-1 ${item.enabled ? "border-emerald-200/30 bg-emerald-950/30 text-emerald-100" : "border-white/20 bg-white/5 text-white/55"}`}>{item.enabled ? "Enabled" : "Disabled"}</span>
                                        </div>
                                        <p className="mt-3 text-xs text-white/55">Novel: {item.novelId ?? "none selected"}</p>
                                        <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-white/85">{item.instruction}</p>
                                    </article>
                                ))}
                            </div>
                        )}
                    </section>
                </div>
            </div>
        </main>
    );
}
