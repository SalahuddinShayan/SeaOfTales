"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import AuthGate from "@/components/auth/AuthGate";

function ForgotForm() {
	const router = useRouter();
	const [identifier, setIdentifier] = useState("");
	const [code, setCode] = useState("");
	const [password, setPassword] = useState("");
	const [sent, setSent] = useState(false);
	const [message, setMessage] = useState("");

	async function requestCode() {
		const response = await fetch("/api/auth/forgot-password", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ identifier }),
		});
		const data = await response.json();
		setMessage(data.message);
		setSent(true);
	}

	async function resetPassword() {
		const response = await fetch("/api/auth/reset-password", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ identifier, code, newPassword: password }),
		});
		if (response.ok) router.push("/login");
		else setMessage((await response.json()).message);
	}

	return (
		<main className="min-h-screen bg-[#07141c] p-8 text-white">
			<div className="mx-auto max-w-md rounded-3xl border border-white/20 bg-black/25 p-8">
				<h1 className="text-3xl">Forgot password</h1>
				{!sent ? (
					<>
						<input
							value={identifier}
							onChange={(event) => setIdentifier(event.target.value)}
							placeholder="Username or email"
							className="mt-5 w-full rounded-xl bg-black/30 p-3"
						/>
						<button onClick={requestCode} className="mt-5 rounded-full bg-white px-5 py-2 text-black">
							Request code
						</button>
					</>
				) : (
					<>
						<p className="mt-4 text-sm">{message}</p>
						<input
							value={code}
							maxLength={6}
							onChange={(event) => setCode(event.target.value)}
							placeholder="6-digit code"
							className="mt-4 w-full rounded-xl bg-black/30 p-3"
						/>
						<input
							type="password"
							value={password}
							onChange={(event) => setPassword(event.target.value)}
							placeholder="New password"
							className="mt-4 w-full rounded-xl bg-black/30 p-3"
						/>
						<button onClick={resetPassword} className="mt-5 rounded-full bg-white px-5 py-2 text-black">
							Reset password
						</button>
					</>
				)}
				{message && !sent && <p className="mt-4 text-sm">{message}</p>}
				<p className="mt-6 text-sm text-white/75">
					<Link href="/login" className="text-white underline underline-offset-4 hover:text-white/80">
						Return to login
					</Link>
				</p>
			</div>
		</main>
	);
}

export default function Forgot() {
	return (
		<AuthGate mode="guest">
			<ForgotForm />
		</AuthGate>
	);
}
