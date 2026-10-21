"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";

interface NovelOptions {
    sourceLanguages: string[];
    statuses: string[];
}

interface NovelForm {
    title: string;
    authorUsername: string;
    sourceLanguage: string;
    description: string;
    
    status: string;
}

export default function NewNovelPage() {
    const [options, setOptions] = useState<NovelOptions>({
        sourceLanguages: [],
        statuses: [],
    });

    const [form, setForm] = useState<NovelForm>({
        title: "",
        authorUsername: "",
        sourceLanguage: "",
        description: "",
        
        status: "",
    });

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState("");
    const [coverImage, setCoverImage] = useState<File | null>(null);
    const coverImageInput = useRef<HTMLInputElement>(null);

    useEffect(() => {
        async function loadOptions() {
            try {
                const response = await fetch("/api/novels/options");

                if (!response.ok) {
                    throw new Error("Failed to load novel options.");
                }

                const data: NovelOptions = await response.json();

                setOptions(data);

                setForm((current) => ({
                    ...current,
                    sourceLanguage: data.sourceLanguages[0] ?? "",
                    status: data.statuses[0] ?? "",
                }));
            } catch (error) {
                setMessage(
                    error instanceof Error
                        ? error.message
                        : "Failed to load novel options."
                );
            } finally {
                setLoading(false);
            }
        }

        loadOptions();
    }, []);

    function handleChange(
        event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
    ) {
        const { name, value } = event.target;

        setForm((current) => ({
            ...current,
            [name]: value,
        }));
    }

    async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>
) {
    event.preventDefault();

    setSubmitting(true);
    setMessage("");

    try {
        const formData = new FormData();

        formData.append("title", form.title);
        formData.append("authorUsername", form.authorUsername);
        formData.append("sourceLanguage", form.sourceLanguage);
        formData.append("description", form.description);
        formData.append("status", form.status);

        if (coverImage) {
            formData.append("coverImage", coverImage);
        }

        const response = await fetch("/api/novels", {
            method: "POST",
            body: formData,
        });

        const responseText = await response.text();

        if (!response.ok) {
            throw new Error(
                `Failed to create novel (${response.status}): ${responseText || "No response body"}`
            );
        }

        const createdNovel = JSON.parse(responseText);

        setMessage(`Novel created successfully. ID: ${createdNovel.id}`);

        setForm({
            title: "",
            authorUsername: "",
            sourceLanguage: options.sourceLanguages[0] ?? "",
            description: "",
            status: options.statuses[0] ?? "",
        });

        setCoverImage(null);
        if (coverImageInput.current) {
            coverImageInput.current.value = "";
        }
    } catch (error) {
        setMessage(
            error instanceof Error
                ? error.message
                : "Failed to create novel."
        );
    } finally {
        setSubmitting(false);
    }
}

    if (loading) {
        return (
            <main className="min-h-screen bg-[#07141c] px-6 py-10 text-white sm:px-10 lg:px-16">
                <p className="text-white/75">Loading novel options...</p>
            </main>
        );
    }

    return (
        <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white">
            <div
                className="absolute inset-0 bg-cover bg-center opacity-25"
                style={{ backgroundImage: "url('/images/sea-night.png')" }}
                aria-hidden="true"
            />
            <div className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]" aria-hidden="true" />

            <div className="relative mx-auto max-w-5xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12">
                <nav className="flex items-center justify-between gap-4">
                    <Link href="/" className="text-sm font-semibold tracking-[0.2em] text-white transition-opacity hover:opacity-75 sm:text-base">
                        SEA OF TALES
                    </Link>
                    <Link href="/admin/novels" className="rounded-full border border-white/35 bg-black/15 px-4 py-2 text-sm text-white backdrop-blur-sm transition hover:bg-white/15">
                        Novel Admin
                    </Link>
                </nav>

                <section className="mt-12 rounded-3xl border border-white/20 bg-black/25 p-6 shadow-2xl shadow-black/30 backdrop-blur-md sm:p-10">
                    <p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">CONTENT MANAGEMENT</p>
                    <h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Create a new novel</h1>
                    <p className="mt-3 max-w-2xl text-sm leading-6 text-white/70 sm:text-base">
                        Set up the source novel that will anchor its chapters, terminology, and translation instructions.
                    </p>

                    <form onSubmit={handleSubmit} className="mt-8 grid gap-6 sm:grid-cols-2">
                        <div>
                            <label htmlFor="title" className="mb-2 block text-sm font-medium text-white/90">Title</label>
                    <input
                        id="title"
                        name="title"
                        type="text"
                        value={form.title}
                        onChange={handleChange}
                        required
                        className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none transition placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"
                    />
                </div>

                <div>
                    <label htmlFor="authorUsername" className="mb-2 block text-sm font-medium text-white/90">Author Username</label>
                    <input
                        id="authorUsername"
                        name="authorUsername"
                        type="text"
                        value={form.authorUsername}
                        onChange={handleChange}
                        required
                        className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none transition placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"
                    />
                </div>

                <div>
                    <label htmlFor="sourceLanguage" className="mb-2 block text-sm font-medium text-white/90">Source Language</label>
                    <select
                        id="sourceLanguage"
                        name="sourceLanguage"
                        value={form.sourceLanguage}
                        onChange={handleChange}
                        required
                        className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none transition focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"
                    >
                        {options.sourceLanguages.map((language) => (
                            <option key={language} value={language}>
                                {language}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="sm:col-span-2">
                    <label htmlFor="description" className="mb-2 block text-sm font-medium text-white/90">Description</label>
                    <textarea
                        id="description"
                        name="description"
                        value={form.description}
                        onChange={handleChange}
                        rows={6}
                        className="w-full resize-y rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none transition placeholder:text-white/35 focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"
                    />
                </div>

                <div>
                    <label htmlFor="coverImage" className="mb-2 block text-sm font-medium text-white/90">Cover Image</label>
                    <input
                        id="coverImage"
                        name="coverImage"
                        type="file"
                        accept="image/*"
                        ref={coverImageInput}
                        onChange={(event) => {
                            setCoverImage(event.target.files?.[0] ?? null);
                        }}
                        className="block w-full cursor-pointer rounded-xl border border-dashed border-white/30 bg-black/20 px-4 py-3 text-sm text-white/75 file:mr-4 file:rounded-lg file:border-0 file:bg-white/15 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-white hover:file:bg-white/25"
                    />
                    <p className="mt-2 text-xs text-white/55">{coverImage ? coverImage.name : "Optional. Choose an image file from your computer."}</p>
                </div>

                <div>
                    <label htmlFor="status" className="mb-2 block text-sm font-medium text-white/90">Status</label>
                    <select
                        id="status"
                        name="status"
                        value={form.status}
                        onChange={handleChange}
                        required
                        className="w-full rounded-xl border border-white/20 bg-black/25 px-4 py-3 text-white outline-none transition focus:border-cyan-100/70 focus:ring-2 focus:ring-cyan-100/20"
                    >
                        {options.statuses.map((status) => (
                            <option key={status} value={status}>
                                {status}
                            </option>
                        ))}
                    </select>
                </div>

                        <div className="sm:col-span-2 flex flex-wrap items-center gap-4 border-t border-white/15 pt-6">
                            <button type="submit" disabled={submitting} className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-60">
                                {submitting ? "Creating..." : "Create Novel"}
                            </button>
                            <Link href="/admin/novels" className="text-sm text-white/75 transition hover:text-white">Return to Novel Admin</Link>
                        </div>
                    </form>

                    {message && (
                        <p className="mt-6 rounded-xl border border-white/15 bg-black/20 px-4 py-3 text-sm text-white/85" role="status">
                            {message}
                        </p>
                    )}
                </section>
            </div>
        </main>
    );
}
