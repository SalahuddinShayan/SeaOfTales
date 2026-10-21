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

interface Terminology {
    id: number;
    scope: string;
    language: string | null;
    novelId: number | null;
    sourceTerm: string;
    translatedTerm: string;
    type: string;
    nameStyle: string | null;
    gender: string | null;
    notes: string | null;
    enabled: boolean;
}

interface TerminologyOptions {
    scopes: string[];
    languages: string[];
    types: string[];
    genders: string[];
    nameStyles: string[];
}

export default function TerminologyPage() {
    const params = useParams<{ novelId?: string }>();
    const routeNovelId = params.novelId ? Number(params.novelId) : null;
    const hasRouteNovel = routeNovelId !== null && Number.isInteger(routeNovelId);

    const [novels, setNovels] = useState<Novel[]>([]);
    const [options, setOptions] = useState<TerminologyOptions>({ scopes: [], languages: [], types: [], genders: [], nameStyles: [] });
    const [entries, setEntries] = useState<Terminology[]>([]);
    const [scope, setScope] = useState(hasRouteNovel ? "NOVEL" : "");
    const [language, setLanguage] = useState("");
    const [novelId, setNovelId] = useState(hasRouteNovel ? String(routeNovelId) : "");
    const [sourceTerm, setSourceTerm] = useState("");
    const [translatedTerm, setTranslatedTerm] = useState("");
    const [type, setType] = useState("");
    const [nameStyle, setNameStyle] = useState("");
    const [gender, setGender] = useState("");
    const [notes, setNotes] = useState("");
    const [enabled, setEnabled] = useState(true);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const selectedNovel = useMemo(() => novels.find((novel) => String(novel.id) === novelId), [novels, novelId]);

    async function loadData() {
        try {
            const [novelsResponse, optionsResponse, terminologyResponse] = await Promise.all([
                fetch("/api/novels"),
                fetch("/api/terminology/options"),
                fetch("/api/terminology"),
            ]);
            if (!novelsResponse.ok || !optionsResponse.ok || !terminologyResponse.ok) {
                throw new Error("Failed to load terminology data.");
            }
            setNovels(await novelsResponse.json());
            setOptions(await optionsResponse.json());
            setEntries(await terminologyResponse.json());
        } catch (loadError) {
            setError(loadError instanceof Error ? loadError.message : "Failed to load terminology data.");
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

        try {
            const response = await fetch("/api/terminology", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    scope,
                    language: language || null,
                    novelId: scope === "NOVEL" ? Number(novelId) : null,
                    sourceTerm,
                    translatedTerm,
                    type,
                    nameStyle: type === "CHARACTER" ? nameStyle || null : null,
                    gender: gender || null,
                    notes: notes || null,
                    enabled,
                }),
            });
            const responseText = await response.text();
            const responseData = responseText ? JSON.parse(responseText) : null;
            if (!response.ok) {
                throw new Error(responseData?.message || "Failed to create terminology.");
            }

            setMessage(`Terminology created successfully. ID: ${responseData.id}`);
            setSourceTerm("");
            setTranslatedTerm("");
            setType("");
            setNameStyle("");
            setGender("");
            setNotes("");
            setEnabled(true);
            if (!hasRouteNovel) {
                setScope("");
                setLanguage("");
                setNovelId("");
            }
            await loadData();
        } catch (submitError) {
            setError(submitError instanceof Error ? submitError.message : "Failed to create terminology.");
        } finally {
            setSubmitting(false);
        }
    }

    function handleScopeChange(value: string) {
        setScope(value);
        if (value === "LANGUAGE" && !hasRouteNovel) setNovelId("");
        if (value === "NOVEL" && hasRouteNovel) setNovelId(String(routeNovelId));
    }

    function handleTypeChange(value: string) {
        setType(value);
        if (value !== "CHARACTER") {
            setNameStyle("");
        }
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
                    <h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Terminology</h1>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-white/70 sm:text-base">Maintain consistent names, places, titles, and other terms for future translation work.</p>
                </header>

                {selectedNovel && <div className="mt-8 rounded-2xl border border-cyan-100/35 bg-cyan-950/30 px-5 py-4 shadow-lg shadow-black/20"><p className="text-xs font-semibold tracking-[0.18em] text-cyan-100/75">SELECTED NOVEL</p><p className="mt-1 text-lg font-medium">{selectedNovel.title}</p><p className="mt-1 text-sm text-white/65">{selectedNovel.authorUsername} · {selectedNovel.sourceLanguage} · ID {selectedNovel.id}</p></div>}

                <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
                    <section className="rounded-3xl border border-white/20 bg-black/25 p-6 shadow-2xl shadow-black/30 backdrop-blur-md sm:p-8">
                        <h2 className="text-xl font-semibold">Add terminology</h2>
                        <p className="mt-2 text-sm text-white/65">Create a language-wide or novel-specific standardized term.</p>
                        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
                            <div><label htmlFor="scope" className="mb-2 block text-sm font-medium text-white/90">Scope</label><select id="scope" value={scope} onChange={(event) => handleScopeChange(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="" disabled>Select a scope</option>{options.scopes.map((value) => <option key={value} value={value}>{value}</option>)}</select></div>
                            <div><label htmlFor="language" className="mb-2 block text-sm font-medium text-white/90">Language{scope === "LANGUAGE" ? " *" : ""}</label><select id="language" value={language} onChange={(event) => setLanguage(event.target.value)} required={scope === "LANGUAGE"} className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="">Select a language</option>{options.languages.map((value) => <option key={value} value={value}>{value}</option>)}</select></div>
                            <div><label htmlFor="novelId" className="mb-2 block text-sm font-medium text-white/90">Novel{scope === "NOVEL" ? " *" : ""}</label><select id="novelId" value={novelId} onChange={(event) => setNovelId(event.target.value)} required={scope === "NOVEL"} disabled={scope !== "NOVEL" || hasRouteNovel} className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none disabled:cursor-not-allowed disabled:opacity-45 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="">{scope === "NOVEL" ? "Select a novel" : "Not used for this scope"}</option>{novels.map((novel) => <option key={novel.id} value={novel.id}>{novel.title}</option>)}</select></div>
                            <div><label htmlFor="sourceTerm" className="mb-2 block text-sm font-medium text-white/90">Source Term</label><input id="sourceTerm" value={sourceTerm} onChange={(event) => setSourceTerm(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" /></div>
                            <div><label htmlFor="translatedTerm" className="mb-2 block text-sm font-medium text-white/90">Translated Term</label><input id="translatedTerm" value={translatedTerm} onChange={(event) => setTranslatedTerm(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" /></div>
                            <div><label htmlFor="type" className="mb-2 block text-sm font-medium text-white/90">Type</label><select id="type" value={type} onChange={(event) => handleTypeChange(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="" disabled>Select a type</option>{options.types.map((value) => <option key={value} value={value}>{value}</option>)}</select></div>
                            {type === "CHARACTER" && <div><label htmlFor="nameStyle" className="mb-2 block text-sm font-medium text-white/90">Name Style</label><select id="nameStyle" value={nameStyle} onChange={(event) => setNameStyle(event.target.value)} required className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="" disabled>Select a name style</option>{options.nameStyles.map((value) => <option key={value} value={value}>{value}</option>)}</select></div>}
                            <div><label htmlFor="gender" className="mb-2 block text-sm font-medium text-white/90">Gender</label><select id="gender" value={gender} onChange={(event) => setGender(event.target.value)} className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"><option value="">Select gender if relevant</option>{options.genders.map((value) => <option key={value} value={value}>{value}</option>)}</select></div>
                            <div><label htmlFor="notes" className="mb-2 block text-sm font-medium text-white/90">Notes</label><textarea id="notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={4} className="w-full resize-y rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20" /></div>
                            <label className="flex items-center gap-3 text-sm text-white/90"><input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} className="h-4 w-4 accent-cyan-200" />Enabled</label>
                            <button type="submit" disabled={submitting || loading} className="w-full rounded-full bg-white px-6 py-3 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-60">{submitting ? "Saving..." : "Create Terminology"}</button>
                        </form>
                        {message && <p className="mt-5 rounded-xl border border-emerald-200/25 bg-emerald-950/30 px-4 py-3 text-sm text-emerald-100" role="status">{message}</p>}
                        {error && <p className="mt-5 rounded-xl border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100" role="alert">{error}</p>}
                    </section>

                    <section className="rounded-3xl border border-white/20 bg-black/25 shadow-2xl shadow-black/30 backdrop-blur-md"><div className="border-b border-white/15 p-6 sm:p-8"><h2 className="text-xl font-semibold">Existing terminology</h2><p className="mt-2 text-sm text-white/65">{loading ? "Loading..." : `${entries.length} entr${entries.length === 1 ? "y" : "ies"} stored`}</p></div>{!loading && entries.length === 0 && <p className="p-6 text-sm text-white/65 sm:p-8">No terminology entries have been created yet.</p>}{!loading && entries.length > 0 && <div className="divide-y divide-white/15">{entries.map((entry) => <article key={entry.id} className="p-6 sm:p-8"><div className="flex flex-wrap items-center gap-2 text-xs font-medium tracking-wide text-white/80"><span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">{entry.scope}</span>{entry.language && <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">{entry.language}</span>}<span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">{entry.type}</span>{entry.type === "CHARACTER" && <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">Name Style: {entry.nameStyle ?? "—"}</span>}{entry.gender && <span className="rounded-full border border-white/20 bg-white/10 px-2.5 py-1">{entry.gender}</span>}<span className={`rounded-full border px-2.5 py-1 ${entry.enabled ? "border-emerald-200/30 bg-emerald-950/30 text-emerald-100" : "border-white/20 bg-white/5 text-white/55"}`}>{entry.enabled ? "Enabled" : "Disabled"}</span></div><p className="mt-4 text-lg font-medium">{entry.sourceTerm} <span className="text-white/45">→</span> {entry.translatedTerm}</p><p className="mt-2 text-xs text-white/55">Novel: {entry.novelId ?? "none selected"}</p>{entry.notes && <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-white/70">{entry.notes}</p>}</article>)}</div>}</section>
                </div>
            </div>
        </main>
    );
}
