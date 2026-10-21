"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import AuthGate from "@/components/auth/AuthGate";

type Form = { username: string; email: string; password: string };

function RegisterForm() {
  const router = useRouter();
  const [form, setForm] = useState<Form>({ username: "", email: "", password: "" });
  const [code, setCode] = useState("");
  const [verificationPending, setVerificationPending] = useState(false);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [profilePicture, setProfilePicture] = useState<File | null>(null);
  const [profilePictureError, setProfilePictureError] = useState("");
  const [profilePreview, setProfilePreview] = useState("");
  const profilePictureInput = useRef<HTMLInputElement>(null);
  useEffect(() => () => { if (profilePreview) URL.revokeObjectURL(profilePreview); }, [profilePreview]);

  async function request(url: string, body: object | FormData) {
    const response = await fetch(url, { method: "POST", ...(body instanceof FormData ? { body } : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }) });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || "Registration could not be completed.");
    return data;
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    if (profilePictureError) return;
    if (form.password.length < 8) { setMessage("Password must be at least 8 characters."); return; }
    setBusy(true);
    try {
      const body = new FormData(); body.set("username", form.username); body.set("email", form.email); body.set("password", form.password); if (profilePicture) body.set("profilePicture", profilePicture);
      const data = await request("/api/auth/register", body);
      if (data.requiresVerification !== true) throw new Error("Registration could not be started. Please try again.");
      setVerificationPending(true);
      setMessage("A verification code was sent to your email.");
    } catch (error) { setMessage(error instanceof Error ? error.message : "Registration could not be completed."); }
    finally { setBusy(false); }
  }

  async function verify() {
    setMessage(""); setBusy(true);
    try { await request("/api/auth/verify-otp", { email: form.email, code }); router.push("/login"); }
    catch (error) { setMessage(error instanceof Error ? error.message : "The verification code is incorrect."); }
    finally { setBusy(false); }
  }

  async function resend() {
    setMessage(""); setBusy(true);
    try { await request("/api/auth/resend-otp", { email: form.email, purpose: "REGISTRATION" }); setMessage("A new verification code was sent."); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Please wait before requesting another code."); }
    finally { setBusy(false); }
  }

  return <main className="min-h-screen bg-[#07141c] p-8 text-white"><div className="mx-auto max-w-md rounded-3xl border border-white/20 bg-black/25 p-8">
    <h1 className="text-3xl font-semibold">{verificationPending ? "Verify your email" : "Create account"}</h1>
    {!verificationPending ? <form onSubmit={submit}>
      {(["username", "email"] as const).map((key) => <input key={key} required placeholder={key} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} className="mt-4 w-full rounded-xl bg-black/30 p-3" />)}
      <input required minLength={8} type="password" placeholder="password (at least 8 characters)" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} className="mt-4 w-full rounded-xl bg-black/30 p-3" />
      <section className="mt-5" aria-labelledby="profile-picture-heading">
        <div className="mb-2 flex items-center gap-2">
          <h2 id="profile-picture-heading" className="text-sm font-semibold text-white/90">Profile picture</h2>
          <span className="rounded-full border border-white/15 bg-white/5 px-2 py-0.5 text-xs text-white/55">Optional</span>
        </div>
        <p id="profile-picture-description" className="mb-3 text-xs leading-relaxed text-white/60">
          Add a picture to personalize your public profile.
        </p>
        <div className="rounded-2xl border border-dashed border-white/20 bg-white/[0.035] p-4 transition focus-within:border-cyan-100/45 focus-within:bg-white/[0.05] focus-within:ring-2 focus-within:ring-cyan-100/20 sm:p-5">
          <input
            ref={profilePictureInput}
            id="profile-picture-input"
            type="file"
            accept="image/png,image/jpeg,image/gif"
            aria-describedby={profilePictureError ? "profile-picture-description profile-picture-help profile-picture-error" : "profile-picture-description profile-picture-help"}
            aria-invalid={!!profilePictureError}
            onClick={(event) => { event.currentTarget.value = ""; }}
            onChange={(event) => {
              const file = event.target.files?.[0] ?? null;
              const invalid = !!file && file.size > 1048576;
              setProfilePictureError(invalid ? "Profile pictures must be 1 MiB or smaller." : "");
              setProfilePicture(invalid ? null : file);
              setProfilePreview(!invalid && file ? URL.createObjectURL(file) : "");
            }}
            className="peer sr-only"
          />
          {profilePicture && profilePreview ? (
            <div className="flex min-w-0 items-center gap-3 sm:gap-4">
              <Image
                src={profilePreview}
                alt={`Selected profile picture${form.username ? ` for ${form.username}` : ""}`}
                width={64}
                height={64}
                unoptimized
                className="h-16 w-16 shrink-0 rounded-full border border-white/20 object-cover shadow-md shadow-black/30"
              />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-white/90">{profilePicture.name}</p>
                <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2">
                  <label
                    htmlFor="profile-picture-input"
                    className="inline-flex cursor-pointer items-center rounded-full bg-white px-3.5 py-2 text-xs font-semibold text-[#07141c] transition hover:bg-cyan-50"
                  >
                    Change picture
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setProfilePicture(null);
                      setProfilePreview("");
                      setProfilePictureError("");
                      if (profilePictureInput.current) profilePictureInput.current.value = "";
                    }}
                    className="rounded-sm text-xs font-medium text-white/65 underline decoration-white/35 underline-offset-4 transition hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-cyan-100"
                    aria-label="Remove selected profile picture"
                  >
                    Remove
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center">
              <span className="grid h-12 w-12 shrink-0 place-items-center rounded-full border border-cyan-100/15 bg-cyan-100/[0.06] text-cyan-100/75" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5" />
                  <path d="M5 14.5v3A1.5 1.5 0 0 0 6.5 19h11a1.5 1.5 0 0 0 1.5-1.5v-3" />
                </svg>
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-white/85">No picture selected</p>
                <label
                  htmlFor="profile-picture-input"
                  className="mt-2 inline-flex cursor-pointer items-center rounded-full bg-white px-4 py-2 text-xs font-semibold text-[#07141c] transition hover:bg-cyan-50"
                >
                  Choose picture
                </label>
              </div>
            </div>
          )}
          <p id="profile-picture-help" className="mt-3 text-xs text-white/45">
            JPEG, PNG, or GIF · Up to 1 MiB
          </p>
        </div>
        {profilePictureError && <p id="profile-picture-error" className="mt-2 text-sm text-red-200" role="alert">{profilePictureError}</p>}
      </section>
      <button disabled={busy} className="mt-6 rounded-full bg-white px-5 py-2 text-black">Register</button>
    </form> : <section>
      <p className="mt-4 text-sm">We sent a verification code to:</p><p className="font-medium">{form.email}</p>
      <input inputMode="numeric" required maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))} placeholder="6-digit verification code" className="mt-4 w-full rounded-xl bg-black/30 p-3" />
      <button disabled={busy} onClick={verify} className="mt-6 rounded-full bg-white px-5 py-2 text-black">Verify</button>
      <button disabled={busy} onClick={resend} className="ml-3 rounded-full border border-white/30 px-5 py-2">Resend Code</button>
    </section>}
    {message && <p className="mt-4 text-red-200">{message}</p>}
    <p className="mt-6 text-sm text-white/75">
      Already have an account?{" "}
      <Link href="/login" className="text-white underline underline-offset-4 hover:text-white/80">
        Login
      </Link>
    </p>
  </div></main>;
}

export default function Register() {
  return (
    <AuthGate mode="guest">
      <RegisterForm />
    </AuthGate>
  );
}
