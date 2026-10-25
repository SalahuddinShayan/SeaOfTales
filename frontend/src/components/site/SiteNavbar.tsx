"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { useSiteTheme } from "@/components/site/SiteThemeProvider";

export default function SiteNavbar() {
  const { status, user, refreshAuth } = useAuth();
  const { theme, toggleTheme } = useSiteTheme();
  const [logoutError, setLogoutError] = useState("");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  async function logout() {
    setLogoutError("");
    try {
      const response = await fetch("/api/auth/logout", { method: "POST", credentials: "same-origin" });
      if (!response.ok) {
        await refreshAuth();
        setLogoutError("Could not log out. Please try again.");
        return;
      }
      await refreshAuth();
    } catch {
      await refreshAuth();
      setLogoutError("Could not log out. Please try again.");
    }
  }

  return (
    <>
      <header className="site-header">
        <div className="site-header__inner">
          <Link className="site-brand" href="/" aria-label="Sea of Tales home">
            <span className="site-brand__mark" aria-hidden="true">S</span>
            <span>SEA OF TALES</span>
          </Link>
          <nav id="site-primary-navigation" className={`site-nav${mobileMenuOpen ? " site-nav--open" : ""}`} aria-label="Primary navigation">
            <Link href="/#featured" onClick={() => setMobileMenuOpen(false)}>Featured</Link>
            <Link href="/#popular" onClick={() => setMobileMenuOpen(false)}>Popular</Link>
            <Link href="/#updates" onClick={() => setMobileMenuOpen(false)}>Latest</Link>
            <Link href="/novels" onClick={() => setMobileMenuOpen(false)}>Novels</Link>
            <Link href="/authors" onClick={() => setMobileMenuOpen(false)}>Authors</Link>
          </nav>
          <button
            className="site-nav-toggle"
            type="button"
            aria-label={mobileMenuOpen ? "Close navigation menu" : "Open navigation menu"}
            aria-expanded={mobileMenuOpen}
            aria-controls="site-primary-navigation"
            onClick={() => setMobileMenuOpen((open) => !open)}
          >
            <span aria-hidden="true">{mobileMenuOpen ? "×" : "☰"}</span>
          </button>
          <div className="site-header__actions">
            {status === "unauthenticated" && <Link className="site-auth-link" href="/login">Login</Link>}
            {status === "authenticated" && (
              <>
                <Link className="site-auth-link" href="/library">Library</Link>
                {user?.role === "ROLE_ADMIN" && <><Link className="site-auth-link" href="/admin/invitations">Admin</Link><Link className="site-auth-link" href="/admin/contact">Inbox</Link><Link className="site-auth-link" href="/admin/comment-reports">Reports</Link></>}
                {user && <Link className="site-username" href={user.role === "ROLE_AUTHOR" ? `/authors/${encodeURIComponent(user.username)}` : `/profiles/${encodeURIComponent(user.username)}`}>{user.username}</Link>}
                <button className="site-auth-link" type="button" onClick={logout}>Logout</button>
              </>
            )}
            <button
              className="theme-toggle"
              type="button"
              onClick={toggleTheme}
              aria-label={theme === "night" ? "Switch to daytime" : "Switch to nighttime"}
              title={theme === "night" ? "Switch to day" : "Switch to night"}
            >
              <span aria-hidden="true">{theme === "night" ? "☾" : "☀"}</span>
            </button>
          </div>
        </div>
      </header>
      {logoutError && <p className="site-alert" role="alert">{logoutError}</p>}
    </>
  );
}
