"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "./AuthProvider";

export default function AuthGate({
  mode,
  children,
}: {
  mode: "guest" | "authenticated" | "admin";
  children: ReactNode;
}) {
  const router = useRouter();
  const { status, user } = useAuth();
  const allowed =
    (mode === "guest" && status === "unauthenticated") ||
    (mode === "authenticated" && status === "authenticated") ||
    (mode === "admin" && status === "authenticated" && user?.role === "ROLE_ADMIN");

  useEffect(() => {
    if (status === "loading") return;
    if (mode === "guest" && status === "authenticated") router.replace("/");
    if (mode === "authenticated" && status === "unauthenticated") {
      router.replace("/login");
    }
    if (mode === "admin" && status === "unauthenticated") router.replace("/login");
    if (mode === "admin" && status === "authenticated" && user?.role !== "ROLE_ADMIN") {
      router.replace("/");
    }
  }, [mode, router, status, user?.role]);

  return allowed ? children : null;
}