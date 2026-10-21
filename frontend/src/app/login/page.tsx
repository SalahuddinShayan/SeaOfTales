"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import AuthGate from "@/components/auth/AuthGate";
import { useAuth } from "@/components/auth/AuthProvider";

function LoginForm() {
	const router = useRouter();
	const { refreshAuth } = useAuth();
	const [form, setForm] = useState({ username: "", password: "" });
	const [error, setError] = useState("");

	async function submit() {
		const response = await fetch("/api/auth/login", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(form),
		});
		if (response.ok) {
			await refreshAuth();
			router.push("/");
		}
		else setError("Invalid credentials.");
	}

	return (
		<main className="min-h-screen bg-[#07141c] p-8 text-white">
			<div className="mx-auto max-w-md rounded-3xl border border-white/20 bg-black/25 p-8">
				<h1 className="text-3xl font-semibold">Log in</h1>
				<input
					placeholder="username"
					value={form.username}
					onChange={(event) => setForm({ ...form, username: event.target.value })}
					className="mt-4 w-full rounded-xl bg-black/30 p-3"
				/>
				<input
					type="password"
					placeholder="password"
					value={form.password}
					onChange={(event) => setForm({ ...form, password: event.target.value })}
					className="mt-4 w-full rounded-xl bg-black/30 p-3"
				/>
				<button
					onClick={submit}
					className="mt-6 rounded-full bg-white px-5 py-2 text-black"
				>
					Log in
				</button>
				{error && <p className="mt-4 text-red-200">{error}</p>}
				<div className="mt-6 space-y-3 text-sm text-white/75">
					<p>
						Don&apos;t have an account?{" "}
						<Link href="/register" className="text-white underline underline-offset-4 hover:text-white/80">
							Create one
						</Link>
					</p>
					<p>
						<Link href="/forgot-password" className="text-white underline underline-offset-4 hover:text-white/80">
							Forgot your password?
						</Link>
					</p>
				</div>
			</div>
		</main>
	);
}

export default function Login() {
	return (
		<AuthGate mode="guest">
			<LoginForm />
		</AuthGate>
	);
}
