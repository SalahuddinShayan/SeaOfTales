"use client";

import Link from "next/link";
import AuthGate from "@/components/auth/AuthGate";
import SiteFooter from "@/components/site/SiteFooter";
import SiteNavbar from "@/components/site/SiteNavbar";

function ProfileEditor() {
  return <div className="min-h-screen"><SiteNavbar /><main className="home-section home-section--popular profile-page"><section className="profile-card"><p className="home-eyebrow">Account</p><h1>Edit Profile</h1><p>Profile editing fields are not available yet. Your username and account security details cannot be changed here.</p><Link href="/account/password">Change Password</Link></section></main><SiteFooter /></div>;
}

export default function ProfileEditorPage() { return <AuthGate mode="authenticated"><ProfileEditor /></AuthGate>; }
