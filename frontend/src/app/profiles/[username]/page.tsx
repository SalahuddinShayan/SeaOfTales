"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";
import ProfileActivitySection from "@/components/profiles/ProfileActivitySection";

type Profile = { username: string; role: string; profilePicture: string | null };

function label(role: string) {
  return role === "ROLE_ADMIN" ? "Administrator" : role === "ROLE_EDITOR" ? "Editor" : "";
}

export default function PublicProfilePage() {
  const { username } = useParams<{ username: string }>();
  const { status, user } = useAuth();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [state, setState] = useState<"loading" | "loaded" | "missing" | "error">("loading");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/profiles/${encodeURIComponent(username)}`, { cache: "no-store", signal: controller.signal })
      .then((response) => {
        if (response.status === 404) { setState("missing"); return null; }
        if (!response.ok) throw new Error();
        return response.json() as Promise<Profile>;
      })
      .then((data) => { if (data) { setProfile(data); setState("loaded"); } })
      .catch((reason: unknown) => { if (!(reason instanceof Error && reason.name === "AbortError")) setState("error"); });
    return () => controller.abort();
  }, [username]);

  const owner = status === "authenticated" && profile !== null && user?.username.toLowerCase() === profile.username.toLowerCase();
  return <div className="min-h-screen"><SiteNavbar /><main className="home-section profile-page">
    {state === "loading" && <p className="novels-feedback" role="status">Loading profile…</p>}
    {state === "error" && <p className="novels-feedback" role="alert">This profile could not be loaded.</p>}
    {state === "missing" && <section className="novels-feedback" role="status"><h1>Profile not found</h1><p>This user profile does not exist.</p><Link href="/">Return home</Link></section>}
    {state === "loaded" && profile && <><section className="profile-card">
      <div className="profile-card__portrait">{profile.profilePicture ? <Image src={profile.profilePicture} alt={`${profile.username} profile picture`} width={128} height={128} unoptimized /> : <span aria-hidden="true">{profile.username.slice(0, 1).toUpperCase()}</span>}</div>
      <p className="home-eyebrow">{label(profile.role) || "Profile"}</p><h1>{profile.username}</h1>
      {label(profile.role) && <p className="profile-card__role">{label(profile.role)}</p>}
      {owner && <nav className="profile-card__actions" aria-label="Profile actions"><Link href="/account/profile">Edit Profile</Link><Link href="/account/password">Change Password</Link></nav>}
    </section><ProfileActivitySection key={profile.username} username={profile.username} /></>}
  </main><SiteFooter /></div>;
}
