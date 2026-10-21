"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

const limits = { name: 100, email: 254, subject: 200, message: 10000 };

export default function ContactPage() {
  const [form, setForm] = useState({ name: "", email: "", subject: "", message: "", website: "" });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  function update(field: keyof typeof form, value: string) { setForm((current) => ({ ...current, [field]: value })); }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setSuccess("");
    if (!form.name.trim() || !form.email.trim() || !form.subject.trim() || !form.message.trim()) { setError("Please complete all required fields."); return; }
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) { setError("Please enter a valid email address."); return; }
    if (form.name.length > limits.name || form.email.length > limits.email || form.subject.length > limits.subject || form.message.length > limits.message) { setError("One or more fields are too long."); return; }
    setBusy(true);
    try {
      const response = await fetch("/api/contact", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(form) });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Your enquiry could not be sent.");
      setSuccess(data.message || "Thank you for contacting Sea of Tales."); setForm({ name: "", email: "", subject: "", message: "", website: "" });
    } catch (submissionError) { setError(submissionError instanceof Error ? submissionError.message : "Your enquiry could not be sent."); } finally { setBusy(false); }
  }
  return <div className="min-h-screen"><SiteNavbar /><main className="contact-page"><section className="contact-card"><p className="home-eyebrow">We would like to hear from you</p><h1>Contact Us</h1><p className="contact-intro">Have a question, enquiry, or idea? Readers, authors, publishers, and other visitors are welcome to get in touch with Sea of Tales.</p>{success && <p className="contact-message contact-message--success" role="status">{success}</p>}{error && <p className="contact-message contact-message--error" role="alert">{error}</p>}<form className="contact-form" onSubmit={(event) => void submit(event)} noValidate><label><span>Name <i aria-hidden="true">*</i></span><input required maxLength={limits.name} value={form.name} onChange={(event) => update("name", event.target.value)} autoComplete="name" /></label><label><span>Email <i aria-hidden="true">*</i></span><input required type="email" maxLength={limits.email} value={form.email} onChange={(event) => update("email", event.target.value)} autoComplete="email" /></label><label><span>Subject <i aria-hidden="true">*</i></span><input required maxLength={limits.subject} value={form.subject} onChange={(event) => update("subject", event.target.value)} /></label><label><span>Message <i aria-hidden="true">*</i></span><textarea required maxLength={limits.message} rows={8} value={form.message} onChange={(event) => update("message", event.target.value)} /></label><div className="contact-honeypot" aria-hidden="true"><label>Website<input tabIndex={-1} autoComplete="off" value={form.website} onChange={(event) => update("website", event.target.value)} /></label></div><button type="submit" disabled={busy}>{busy ? "Sending…" : "Send enquiry"}</button><p className="contact-privacy">Submitted information will be used to handle and respond to your enquiry. See our <Link href="/privacy">privacy notice</Link>.</p></form></section></main><SiteFooter /></div>;
}
