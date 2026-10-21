"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useParams, useSearchParams } from "next/navigation";
import LanguageSelector, { type AppLanguage } from "@/components/auth/LanguageSelector";
import Image from "next/image";

type Language = AppLanguage;
type CopyKey = keyof typeof copy.JAPANESE;
type ValidationState = "loading" | "valid" | "invalid" | "error";
type Form = { username: string; email: string; password: string; confirmPassword: string };

const copy = {
  ENGLISH: {
    pageTitle: "Create your Author account", verifyTitle: "Verify your email", successTitle: "Account created",
    intro: "Welcome to Sea of Tales Author registration. Use the email address that received this invitation.",
    loading: "Checking your invitation…", invalidInvite: "This invitation link is invalid, expired, or has already been used.",
    askAdmin: "Ask the administrator who invited you for a new invitation link.", network: "We couldn't check this invitation. Check your connection and reload the page.",
    username: "Username", email: "Email address", password: "Password", confirmPassword: "Confirm password",
    languageLabel: "Language:", selectLanguage: "Select a language",
    continue: "Continue registration", sending: "Sending…", otpTitle: "Email verification", otpInstructions: "We sent a verification code to this email address.",
    otpCode: "6-digit verification code", verify: "Verify and create account", verifying: "Verifying…", resend: "Resend code", resendSent: "A new verification code was sent.",
    accountCreated: "Your email is verified and your Author account has been created.", goLogin: "Go to Login", already: "Already have an account?",
    invalidForm: "Enter a username, email address, and password of at least 8 characters.", invalidEmail: "Enter a valid email address.",
    passwordShort: "Password must be at least 8 characters.", passwordMismatch: "Passwords do not match.", languageRequired: "Select a language.",
    emailMismatch: "Use the email address that received this invitation.", duplicate: "This username or email address is already in use.",
    otpInvalid: "The verification code is incorrect or expired. Please try again.", registrationExpired: "Your registration expired. Ask the administrator for a new invitation.",
    registrationMissing: "Registration details were not found or have expired. Start again from your invitation link.", rateLimit: "A code was already sent. Check your email before trying again.",
    serverError: "We couldn't start registration. Please try again later.", mailError: "We couldn't send the verification email. Please try again later.",
    profilePicture: "Profile picture", optional: "Optional", profilePictureDescription: "Add a picture to personalize your public profile.",
    noPictureSelected: "No picture selected", choosePicture: "Choose picture", changePicture: "Change picture", removePicture: "Remove",
    selectedProfilePicture: "Selected profile picture", profilePictureHelp: "JPEG, PNG, or GIF · Up to 1 MiB", pictureTooLarge: "Profile pictures must be 1 MiB or smaller.",
  },
  JAPANESE: {
    pageTitle: "作家アカウントを作成", verifyTitle: "メールアドレスを確認", successTitle: "アカウントを作成しました",
    intro: "Sea of Tales の作家登録へようこそ。招待を受け取ったメールアドレスを入力してください。",
    loading: "招待を確認しています…", invalidInvite: "この招待リンクは無効か、有効期限が切れているか、すでに使用されています。",
    askAdmin: "招待を送った管理者に、新しい招待リンクをお問い合わせください。", network: "招待を確認できませんでした。通信状況を確認して、ページを再読み込みしてください。",
    username: "ユーザー名", email: "メールアドレス", password: "パスワード", confirmPassword: "パスワード（確認）",
    languageLabel: "言語:", selectLanguage: "言語を選択してください",
    continue: "登録を続ける", sending: "送信中…", otpTitle: "メール認証", otpInstructions: "認証コードを次のメールアドレスに送信しました。",
    otpCode: "6桁の認証コード", verify: "認証して登録", verifying: "確認中…", resend: "コードを再送信", resendSent: "新しい認証コードを送信しました。",
    accountCreated: "メール認証が完了し、作家アカウントが作成されました。", goLogin: "ログインへ", already: "すでにアカウントをお持ちですか？",
    invalidForm: "ユーザー名、メールアドレス、8文字以上のパスワードを入力してください。", invalidEmail: "有効なメールアドレスを入力してください。",
    passwordShort: "パスワードは8文字以上で入力してください。", passwordMismatch: "パスワードが一致しません。", languageRequired: "表示言語を選択してください。",
    emailMismatch: "招待を受け取ったメールアドレスを使用してください。", duplicate: "このユーザー名またはメールアドレスはすでに使用されています。",
    otpInvalid: "認証コードが正しくないか、有効期限が切れています。もう一度お試しください。", registrationExpired: "登録の有効期限が切れました。管理者に新しい招待を依頼してください。",
    registrationMissing: "登録情報が見つからないか、有効期限が切れています。招待リンクからやり直してください。", rateLimit: "コードはすでに送信されています。メールを確認してから再度お試しください。",
    serverError: "登録を開始できませんでした。時間をおいてもう一度お試しください。", mailError: "認証メールを送信できませんでした。しばらくしてから再度お試しください。",
    profilePicture: "プロフィール画像", optional: "任意", profilePictureDescription: "公開プロフィールを彩る画像を追加できます。", noPictureSelected: "画像が選択されていません", choosePicture: "画像を選ぶ", changePicture: "画像を変更", removePicture: "削除",
    selectedProfilePicture: "選択したプロフィール画像", profilePictureHelp: "JPEG、PNG、GIF · 1 MiB 以下", pictureTooLarge: "プロフィール画像は1 MiB以下にしてください。",
  },
  KOREAN: {
    pageTitle: "작가 계정 만들기", verifyTitle: "이메일 인증", successTitle: "계정이 생성되었습니다",
    intro: "Sea of Tales 작가 등록을 환영합니다. 초대를 받은 이메일 주소를 입력해 주세요.",
    loading: "초대를 확인하는 중…", invalidInvite: "초대 링크가 유효하지 않거나 만료되었거나 이미 사용되었습니다.",
    askAdmin: "초대를 보낸 관리자에게 새 초대 링크를 요청해 주세요.", network: "초대를 확인할 수 없습니다. 연결을 확인한 뒤 페이지를 새로고침해 주세요.",
    username: "사용자 이름", email: "이메일 주소", password: "비밀번호", confirmPassword: "비밀번호 확인",
    languageLabel: "언어:", selectLanguage: "언어 선택",
    continue: "등록 계속하기", sending: "전송 중…", otpTitle: "이메일 인증", otpInstructions: "다음 이메일 주소로 인증 코드를 보냈습니다.",
    otpCode: "6자리 인증 코드", verify: "인증하고 계정 만들기", verifying: "확인 중…", resend: "코드 다시 보내기", resendSent: "새 인증 코드를 보냈습니다.",
    accountCreated: "이메일 인증이 완료되어 작가 계정이 생성되었습니다.", goLogin: "로그인으로 이동", already: "이미 계정이 있으신가요?",
    invalidForm: "사용자 이름, 이메일 주소, 8자 이상의 비밀번호를 입력해 주세요.", invalidEmail: "유효한 이메일 주소를 입력해 주세요.",
    passwordShort: "비밀번호는 8자 이상이어야 합니다.", passwordMismatch: "비밀번호가 일치하지 않습니다.", languageRequired: "화면 언어를 선택해 주세요.",
    emailMismatch: "초대를 받은 이메일 주소를 사용해 주세요.", duplicate: "이미 사용 중인 사용자 이름 또는 이메일 주소입니다.",
    otpInvalid: "인증 코드가 올바르지 않거나 만료되었습니다. 다시 시도해 주세요.", registrationExpired: "등록이 만료되었습니다. 관리자에게 새 초대를 요청해 주세요.",
    registrationMissing: "등록 정보를 찾을 수 없거나 만료되었습니다. 초대 링크에서 다시 시작해 주세요.", rateLimit: "인증 코드를 이미 보냈습니다. 이메일을 확인한 후 다시 시도해 주세요.",
    serverError: "등록을 시작할 수 없습니다. 잠시 후 다시 시도해 주세요.", mailError: "인증 이메일을 보낼 수 없습니다. 잠시 후 다시 시도해 주세요.",
    profilePicture: "프로필 사진", optional: "선택 사항", profilePictureDescription: "공개 프로필을 더욱 개성 있게 꾸며 보세요.", noPictureSelected: "선택된 사진이 없습니다", choosePicture: "사진 선택", changePicture: "사진 변경", removePicture: "삭제",
    selectedProfilePicture: "선택한 프로필 사진", profilePictureHelp: "JPEG, PNG, GIF · 최대 1 MiB", pictureTooLarge: "프로필 사진은 1 MiB 이하여야 합니다.",
  },
} satisfies Record<Language, Record<string, string>>;

function apiErrorKey(message: string): CopyKey {
  const normalized = message.toLowerCase();
  if (message.includes("different email") || message.includes("received this invitation")) return "emailMismatch";
  if (message.includes("Account already exists") || message.includes("already in use")) return "duplicate";
  if (message.includes("Invalid or expired code")) return "otpInvalid";
  if (message.includes("Registration expired")) return "registrationExpired";
  if (message.includes("Registration not found")) return "registrationMissing";
  if (message.includes("wait before") || message.includes("already been sent")) return "rateLimit";
  if (normalized.includes("valid username")) return "invalidForm";
  if (normalized.includes("mail")) return "mailError";
  return "serverError";
}

export default function InvitePage() {
  const { type } = useParams<{ type: string }>();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const kind = type.toUpperCase();
  const [validation, setValidation] = useState<ValidationState>(token && kind === "AUTHOR" ? "loading" : "invalid");
  const [language, setLanguage] = useState<Language>("JAPANESE");
  const languageChanged = useRef(false);
  const [form, setForm] = useState<Form>({ username: "", email: "", password: "", confirmPassword: "" });
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [errorKey, setErrorKey] = useState<CopyKey | "">("");
  const [statusKey, setStatusKey] = useState<CopyKey | "">("");
  const [success, setSuccess] = useState(false);
  const [profilePicture, setProfilePicture] = useState<File | null>(null);
  const [profilePreview, setProfilePreview] = useState("");
  const [profilePictureError, setProfilePictureError] = useState<CopyKey | "">("");
  const profilePictureInput = useRef<HTMLInputElement>(null);
  useEffect(() => () => { if (profilePreview) URL.revokeObjectURL(profilePreview); }, [profilePreview]);
  const t = copy[language];

  useEffect(() => {
    let active = true;
    if (kind !== "AUTHOR" || !token) return () => { active = false; };
    fetch(`/api/invitations/validate?${new URLSearchParams({ token, type: "AUTHOR" })}`)
      .then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (!active) return;
        if (response.ok && data.valid === true && data.type === "AUTHOR") {
          setValidation("valid");
          if (!languageChanged.current && (data.preferredLanguage === "ENGLISH" || data.preferredLanguage === "JAPANESE" || data.preferredLanguage === "KOREAN")) setLanguage(data.preferredLanguage);
        } else {
          setValidation("invalid");
          setErrorKey("invalidInvite");
        }
      })
      .catch(() => {
        if (active) { setValidation("error"); setErrorKey("network"); }
      });
    return () => { active = false; };
  }, [kind, token]);

  useEffect(() => {
    document.documentElement.lang = language === "JAPANESE" ? "ja" : language === "KOREAN" ? "ko" : "en";
  }, [language]);

  async function request(url: string, body: object | FormData) {
    const response = await fetch(url, { method: "POST", ...(body instanceof FormData ? { body } : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }) });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || "Request failed");
    return data;
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setErrorKey(""); setStatusKey("");
    if (!/^[A-Za-z0-9_]{3,50}$/.test(form.username)) { setErrorKey("invalidForm"); return; }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) { setErrorKey("invalidEmail"); return; }
    if (form.password.length < 8) { setErrorKey("passwordShort"); return; }
    if (form.password !== form.confirmPassword) { setErrorKey("passwordMismatch"); return; }
    if (profilePictureError) return;
    setBusy(true);
    try {
      const body = new FormData(); body.set("token", token); body.set("type", "AUTHOR"); body.set("username", form.username); body.set("email", form.email); body.set("password", form.password); body.set("language", language); if (profilePicture) body.set("profilePicture", profilePicture);
      await request("/api/auth/invitation-register", body);
      setSent(true);
    } catch (submitError) { setErrorKey(apiErrorKey(submitError instanceof Error ? submitError.message : "")); }
    finally { setBusy(false); }
  }

  async function verify() {
    setBusy(true); setErrorKey(""); setStatusKey("");
    try { await request("/api/auth/verify-otp", { email: form.email, code }); setSuccess(true); }
    catch (verifyError) { setErrorKey(apiErrorKey(verifyError instanceof Error ? verifyError.message : "")); }
    finally { setBusy(false); }
  }

  async function resend() {
    setBusy(true); setErrorKey(""); setStatusKey("");
    try { await request("/api/auth/resend-otp", { email: form.email, purpose: "AUTHOR_REGISTRATION" }); setStatusKey("resendSent"); }
    catch (resendError) { setErrorKey(apiErrorKey(resendError instanceof Error ? resendError.message : "")); }
    finally { setBusy(false); }
  }

  const title = success ? t.successTitle : sent ? t.verifyTitle : t.pageTitle;

  return (
    <main lang={language === "JAPANESE" ? "ja" : language === "KOREAN" ? "ko" : "en"} className="min-h-screen bg-[#07141c] p-6 text-white sm:p-8">
      <div className="mx-auto max-w-md rounded-3xl border border-white/20 bg-black/25 p-8">
        <div className="flex items-center justify-between gap-4"><p className="text-xs font-semibold tracking-[0.24em] text-cyan-100/75">SEA OF TALES</p><LanguageSelector id="interface-language" value={language} interfaceLanguage={language} onChange={(nextLanguage) => { if (nextLanguage) { languageChanged.current = true; setLanguage(nextLanguage); } }} /></div>
        <h1 className="mt-4 text-3xl font-semibold">{title}</h1>
        {!sent && !success && <p className="mt-3 text-sm text-white/75">{t.intro}</p>}

        {validation === "loading" && <p className="mt-5" role="status">{t.loading}</p>}
        {(validation === "invalid" || validation === "error") && <section className="mt-5" role="alert"><p className="text-red-200">{t[errorKey || "invalidInvite"]}</p><p className="mt-4 text-sm text-white/75">{t.askAdmin}</p></section>}

        {validation === "valid" && !sent && !success && <form noValidate className="mt-5 space-y-4" onSubmit={(event) => void submit(event)}>
          <div><label htmlFor="username" className="mb-1 block text-sm">{t.username}</label><input id="username" autoComplete="username" required minLength={3} maxLength={50} pattern="[A-Za-z0-9_]+" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} className="w-full rounded-xl border border-white/20 bg-black/30 p-3" /></div>
          <div><label htmlFor="email" className="mb-1 block text-sm">{t.email}</label><input id="email" type="email" autoComplete="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="w-full rounded-xl border border-white/20 bg-black/30 p-3" /></div>
          <div><label htmlFor="password" className="mb-1 block text-sm">{t.password}</label><input id="password" type="password" autoComplete="new-password" required minLength={8} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} className="w-full rounded-xl border border-white/20 bg-black/30 p-3" /></div>
          <div><label htmlFor="confirmPassword" className="mb-1 block text-sm">{t.confirmPassword}</label><input id="confirmPassword" type="password" autoComplete="new-password" required minLength={8} value={form.confirmPassword} onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} className="w-full rounded-xl border border-white/20 bg-black/30 p-3" /></div>
          <section className="mt-5" aria-labelledby="profile-picture-heading">
            <div className="mb-2 flex items-center gap-2">
              <h2 id="profile-picture-heading" className="text-sm font-semibold text-white/90">{t.profilePicture}</h2>
              <span className="rounded-full border border-white/15 bg-white/5 px-2 py-0.5 text-xs text-white/55">{t.optional}</span>
            </div>
            <p id="profile-picture-description" className="mb-3 text-xs leading-relaxed text-white/60">{t.profilePictureDescription}</p>
            <div className="rounded-2xl border border-dashed border-white/20 bg-white/[0.035] p-4 transition focus-within:border-cyan-100/45 focus-within:bg-white/[0.05] focus-within:ring-2 focus-within:ring-cyan-100/20 sm:p-5">
              <input
                ref={profilePictureInput}
                id="profilePicture"
                type="file"
                accept="image/png,image/jpeg,image/gif"
                aria-describedby={profilePictureError ? "profile-picture-description profile-picture-help profile-picture-error" : "profile-picture-description profile-picture-help"}
                aria-invalid={!!profilePictureError}
                onClick={(event) => { event.currentTarget.value = ""; }}
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  const invalid = !!file && file.size > 1048576;
                  setProfilePictureError(invalid ? "pictureTooLarge" : "");
                  setProfilePicture(invalid ? null : file);
                  setProfilePreview(!invalid && file ? URL.createObjectURL(file) : "");
                }}
                className="peer sr-only"
              />
              {profilePicture && profilePreview ? (
                <div className="flex min-w-0 items-center gap-3 sm:gap-4">
                  <Image src={profilePreview} alt={`${t.selectedProfilePicture}${form.username ? `: ${form.username}` : ""}`} width={64} height={64} unoptimized className="h-16 w-16 shrink-0 rounded-full border border-white/20 object-cover shadow-md shadow-black/30" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-white/90">{profilePicture.name}</p>
                    <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2">
                      <label htmlFor="profilePicture" className="inline-flex cursor-pointer items-center rounded-full bg-white px-3.5 py-2 text-xs font-semibold text-[#07141c] transition hover:bg-cyan-50">{t.changePicture}</label>
                      <button type="button" onClick={() => { setProfilePicture(null); setProfilePreview(""); setProfilePictureError(""); if (profilePictureInput.current) profilePictureInput.current.value = ""; }} className="rounded-sm text-xs font-medium text-white/65 underline decoration-white/35 underline-offset-4 transition hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-cyan-100" aria-label={t.removePicture}>{t.removePicture}</button>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center">
                  <span className="grid h-12 w-12 shrink-0 place-items-center rounded-full border border-cyan-100/15 bg-cyan-100/[0.06] text-cyan-100/75" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"><path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5" /><path d="M5 14.5v3A1.5 1.5 0 0 0 6.5 19h11a1.5 1.5 0 0 0 1.5-1.5v-3" /></svg>
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-white/85">{t.noPictureSelected}</p>
                    <label htmlFor="profilePicture" className="mt-2 inline-flex cursor-pointer items-center rounded-full bg-white px-4 py-2 text-xs font-semibold text-[#07141c] transition hover:bg-cyan-50">{t.choosePicture}</label>
                  </div>
                </div>
              )}
              <p id="profile-picture-help" className="mt-3 text-xs text-white/45">{t.profilePictureHelp}</p>
            </div>
            {profilePictureError && <p id="profile-picture-error" className="mt-2 text-sm text-red-200" role="alert">{t[profilePictureError]}</p>}
          </section>
          <button type="submit" disabled={busy} className="rounded-full bg-white px-5 py-2 font-semibold text-black disabled:opacity-50">{busy ? t.sending : t.continue}</button>
        </form>}

        {validation === "valid" && sent && !success && <section className="mt-5"><h2 className="text-lg font-medium">{t.otpTitle}</h2><p className="mt-2 text-sm">{t.otpInstructions}</p><p className="mt-1 font-medium">{form.email}</p><label htmlFor="code" className="mb-1 mt-4 block text-sm">{t.otpCode}</label><input id="code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} pattern="[0-9]{6}" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))} className="w-full rounded-xl border border-white/20 bg-black/30 p-3" /><div className="mt-5 flex flex-wrap gap-3"><button type="button" disabled={busy || code.length !== 6} onClick={() => void verify()} className="rounded-full bg-white px-5 py-2 font-semibold text-black disabled:opacity-50">{busy ? t.verifying : t.verify}</button><button type="button" disabled={busy} onClick={() => void resend()} className="rounded-full border border-white/30 px-5 py-2 disabled:opacity-50">{t.resend}</button></div></section>}

        {errorKey && validation === "valid" && <p className="mt-4 text-sm text-red-200" role="alert">{t[errorKey]}</p>}
        {statusKey && <p className="mt-4 text-sm text-emerald-200" role="status">{t[statusKey]}</p>}
        {success && <section className="mt-5" role="status"><p>{t.accountCreated}</p><Link href="/login" className="mt-5 inline-block rounded-full bg-white px-5 py-2 font-semibold text-black">{t.goLogin}</Link></section>}
        <p className="mt-6 text-sm text-white/75">{t.already} <Link href="/login" className="underline underline-offset-4">{t.goLogin}</Link></p>
      </div>
    </main>
  );
}
