"use client";

import { createContext, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";

type SiteTheme = "day" | "night";
type SiteThemeValue = { theme: SiteTheme; toggleTheme: () => void };

const SiteThemeContext = createContext<SiteThemeValue | null>(null);
const THEME_STORAGE_KEY = "sea-of-tales-theme";
const THEME_EVENT = "sea-of-tales-theme-change";

function subscribeToTheme(callback: () => void) {
  window.addEventListener(THEME_EVENT, callback);
  return () => window.removeEventListener(THEME_EVENT, callback);
}

function getThemeSnapshot(): SiteTheme {
  return document.documentElement.dataset.theme === "night" ? "night" : "day";
}

function getServerThemeSnapshot(): SiteTheme {
  return "day";
}

function toggleStoredTheme() {
  const nextTheme = getThemeSnapshot() === "day" ? "night" : "day";
  document.documentElement.dataset.theme = nextTheme;
  window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
  window.dispatchEvent(new Event(THEME_EVENT));
}

export function SiteThemeProvider({ children }: Readonly<{ children: ReactNode }>) {
  const theme = useSyncExternalStore(subscribeToTheme, getThemeSnapshot, getServerThemeSnapshot);
  const value = useMemo(() => ({ theme, toggleTheme: toggleStoredTheme }), [theme]);

  useEffect(() => {
    const initialTheme: SiteTheme = window.localStorage.getItem(THEME_STORAGE_KEY) === "night" ? "night" : "day";
    document.documentElement.dataset.theme = initialTheme;
    window.dispatchEvent(new Event(THEME_EVENT));
  }, []);

  return <SiteThemeContext.Provider value={value}>{children}</SiteThemeContext.Provider>;
}

export function useSiteTheme() {
  const context = useContext(SiteThemeContext);
  if (!context) throw new Error("useSiteTheme must be used within SiteThemeProvider.");
  return context;
}