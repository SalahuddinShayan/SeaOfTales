"use client";

import Link from "next/link";
import { useState } from "react";
import AuthGate from "@/components/auth/AuthGate";
import LanguageSelector, { type AppLanguage } from "@/components/auth/LanguageSelector";

type InvitationType = "AUTHOR" | "EDITOR";
type PreferredLanguage = AppLanguage;

type GeneratedInvitation = {
  type: InvitationType;
  token: string;
  expiresAt: string;
  used: boolean;
  url: string;
  validation: "idle" | "checking" | "valid" | "invalid";
  copied: boolean;
  recipientEmail: string;
  emailSent: boolean;
  preferredLanguage: PreferredLanguage;
};

const invitePaths: Record<InvitationType, string> = {
  AUTHOR: "/invite/author",
  EDITOR: "/invite/editor",
};

function InvitationsDashboard() {
  const [invitations, setInvitations] = useState<
    Partial<Record<InvitationType, GeneratedInvitation>>
  >({});
  const [busy, setBusy] = useState<InvitationType | null>(null);
  const [error, setError] = useState("");
  const [authorEmail, setAuthorEmail] = useState("");
  const [emailValidation, setEmailValidation] = useState("");
  const [preferredLanguage, setPreferredLanguage] = useState<PreferredLanguage | "">("");

  async function generate(type: InvitationType) {
    if (type === "EDITOR") return;
    const normalizedEmail = authorEmail.trim().toLowerCase();
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(normalizedEmail)) {
      setEmailValidation("Enter a valid recipient email address.");
      return;
    }
    if (!preferredLanguage) {
      setEmailValidation("Select a preferred language.");
      return;
    }
    setEmailValidation("");
    setBusy(type);
    setError("");
    try {
      const response = await fetch(`/api/invitations/${type}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: normalizedEmail, language: preferredLanguage }),
        credentials: "same-origin",
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || "Invitation could not be generated.");
      }

      const url = new URL(invitePaths[type], window.location.origin);
      url.searchParams.set("token", data.token);
      setInvitations((current) => ({
        ...current,
        [type]: {
          type,
          token: data.token,
          expiresAt: data.expiresAt,
          used: data.used,
          url: url.toString(),
          validation: "idle",
          copied: false,
          recipientEmail: data.recipientEmail,
          emailSent: data.emailSent === true,
          preferredLanguage: data.preferredLanguage,
        },
      }));
    } catch (generationError) {
      setError(
        generationError instanceof Error
          ? generationError.message
          : "Invitation could not be generated.",
      );
    } finally {
      setBusy(null);
    }
  }

  async function resendAuthor() {
    const invitation = invitations.AUTHOR;
    if (!invitation) return;
    setBusy("AUTHOR");
    setError("");
    try {
      const response = await fetch("/api/invitations/author/resend", {
        method: "POST", headers: { "Content-Type": "application/json" },
        credentials: "same-origin", body: JSON.stringify({ token: invitation.token }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Invitation email could not be resent.");
      setInvitations((current) => ({ ...current, AUTHOR: { ...invitation, emailSent: data.emailSent === true } }));
    } catch (resendError) {
      setError(resendError instanceof Error ? resendError.message : "Invitation email could not be resent.");
    } finally { setBusy(null); }
  }

  async function validate(type: InvitationType) {
    const invitation = invitations[type];
    if (!invitation) return;
    setInvitations((current) => ({
      ...current,
      [type]: { ...invitation, validation: "checking" },
    }));

    try {
      const query = new URLSearchParams({ token: invitation.token, type });
      const response = await fetch(`/api/invitations/validate?${query}`);
      const result = await response.json();
      setInvitations((current) => ({
        ...current,
        [type]: {
          ...invitation,
          validation: response.ok && result.valid ? "valid" : "invalid",
        },
      }));
    } catch {
      setInvitations((current) => ({
        ...current,
        [type]: { ...invitation, validation: "invalid" },
      }));
    }
  }

  async function copy(type: InvitationType) {
    const invitation = invitations[type];
    if (!invitation) return;
    try {
      await navigator.clipboard.writeText(invitation.url);
      setInvitations((current) => ({
        ...current,
        [type]: { ...invitation, copied: true },
      }));
    } catch {
      setError("Could not copy the invitation URL.");
    }
  }

  function renderInvitation(type: InvitationType, title: string) {
    const invitation = invitations[type];
    return (
      <section className="rounded-xl border border-white/15 bg-black/20 p-5 sm:p-6">
        <h2 className="text-lg font-semibold">{title}</h2>
        {type === "AUTHOR" && <div className="mt-4 space-y-4"><div><label htmlFor="author-recipient-email" className="mb-1 block text-sm text-white/85">Recipient email address</label><input id="author-recipient-email" type="email" autoComplete="email" required value={authorEmail} onChange={(event) => { setAuthorEmail(event.target.value); setEmailValidation(""); }} className="w-full rounded-lg border border-white/15 bg-black/30 p-3 text-white" aria-describedby="author-email-validation" /></div><LanguageSelector id="author-preferred-language" value={preferredLanguage} placeholder="Select a language" interfaceLanguage="ENGLISH" onChange={(language) => { setPreferredLanguage(language); setEmailValidation(""); }} />{emailValidation && <p id="author-email-validation" className="text-sm text-red-200" role="alert">{emailValidation}</p>}</div>}
        <button
          type="button"
          disabled={busy !== null || type === "EDITOR"}
          onClick={() => void generate(type)}
          className="mt-4 rounded-full bg-white px-4 py-2 text-sm font-semibold text-[#07141c] transition hover:bg-cyan-50 disabled:cursor-wait disabled:opacity-60"
        >
          {type === "EDITOR" ? "Editor invitations unavailable" : busy === type ? "Generating and emailing..." : `Generate ${title}`}
        </button>
        {invitation && (
          <div className="mt-5 space-y-3 text-sm">
            <label className="block">
              <span className="mb-1 block text-white/65">Invitation URL</span>
              <input
                readOnly
                value={invitation.url}
                className="w-full rounded-lg border border-white/15 bg-black/30 p-3 text-white"
                aria-label={`${title} URL`}
              />
            </label>
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() => void copy(type)}
                className="rounded-full border border-white/30 px-4 py-2 text-sm text-white transition hover:bg-white/10"
              >
                {invitation.copied ? "Copied" : "Copy URL"}
              </button>
              <span className="text-white/70">
                Expires: {new Date(invitation.expiresAt).toLocaleString()}
              </span>
            </div>
            <p className="text-white/70">
              Status: {invitation.used ? "Used" : "Unused"}
            </p>
            {type === "AUTHOR" && <><p className="text-white/70">Preferred language: {invitation.preferredLanguage === "KOREAN" ? "Korean" : invitation.preferredLanguage === "JAPANESE" ? "Japanese" : "English"}</p><p className={invitation.emailSent ? "text-emerald-200" : "text-amber-200"} role="status">{invitation.emailSent ? `Invitation email sent to ${invitation.recipientEmail}.` : `Invitation created, but email delivery failed for ${invitation.recipientEmail}.`}</p>{!invitation.emailSent && <button type="button" disabled={busy !== null || invitation.used} onClick={() => void resendAuthor()} className="rounded-full border border-white/30 px-4 py-2 text-sm text-white transition hover:bg-white/10 disabled:opacity-60">{busy === "AUTHOR" ? "Sending..." : "Resend invitation email"}</button>}</>}
            <button
              type="button"
              disabled={invitation.validation === "checking"}
              onClick={() => void validate(type)}
              className="rounded-full border border-white/30 px-4 py-2 text-sm text-white transition hover:bg-white/10 disabled:opacity-60"
            >
              {invitation.validation === "checking"
                ? "Checking..."
                : "Validate invitation"}
            </button>
            {invitation.validation !== "idle" &&
              invitation.validation !== "checking" && (
                <output className="block text-white/80">
                  {invitation.validation === "valid"
                    ? `Valid ${type.toLowerCase()} invitation`
                    : "Invitation validation failed"}
                </output>
              )}
          </div>
        )}
      </section>
    );
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-[#07141c] text-white">
      <div
        className="absolute inset-0 bg-cover bg-center opacity-25"
        style={{ backgroundImage: "url('/images/sea-night.png')" }}
        aria-hidden="true"
      />
      <div
        className="absolute inset-0 bg-linear-to-b from-black/50 via-[#07141c]/80 to-[#07141c]"
        aria-hidden="true"
      />
      <div className="relative mx-auto max-w-5xl px-6 py-8 sm:px-10 lg:px-16 lg:py-12">
        <nav className="flex items-center justify-between gap-4">
          <Link
            href="/"
            className="text-sm font-semibold tracking-[0.2em] text-white transition-opacity hover:opacity-75 sm:text-base"
          >
            SEA OF TALES
          </Link>
          <span className="text-sm text-white/70">Admin</span>
        </nav>
        <header className="mt-12">
          <p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">
            ADMINISTRATION
          </p>
          <h1 className="mt-3 text-3xl font-semibold sm:text-4xl">
            Invitation Management
          </h1>
          <p className="mt-3 text-sm text-white/70">
            Generate and email time-limited Author invitation links. Editor invitations are unavailable until Editor registration is implemented.
          </p>
        </header>
        {error && (
          <p
            role="alert"
            className="mt-6 rounded-lg border border-red-200/25 bg-red-950/30 px-4 py-3 text-sm text-red-100"
          >
            {error}
          </p>
        )}
        <div className="mt-8 grid gap-4 md:grid-cols-2">
          {renderInvitation("AUTHOR", "Author Invitation")}
          {renderInvitation("EDITOR", "Editor Invitation")}
        </div>
      </div>
    </main>
  );
}

export default function AdminInvitationsPage() {
  return (
    <AuthGate mode="admin">
      <InvitationsDashboard />
    </AuthGate>
  );
}
