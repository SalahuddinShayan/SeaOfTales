"use client";

import AuthGate from "@/components/auth/AuthGate";
import { useState } from "react";

function ChangePasswordForm() {
	const [form, setForm] = useState({ currentPassword: "", newPassword: "" });
	const [message, setMessage] = useState("");

	async function submit() {
		const response = await fetch("/api/auth/change-password", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(form),
		});
		setMessage(
			response.ok ? "Password changed." : (await response.json()).message,
		);
	}

	return (
		<main className="min-h-screen bg-[#07141c] p-8 text-white">
			<div className="mx-auto max-w-md rounded-3xl border border-white/20 bg-black/25 p-8">
				<h1 className="text-3xl">Change password</h1>
				<input
					type="password"
					placeholder="Current password"
					value={form.currentPassword}
					onChange={(event) =>
						setForm({ ...form, currentPassword: event.target.value })
					}
					className="mt-5 w-full rounded-xl bg-black/30 p-3"
				/>
				<input
					type="password"
					placeholder="New password"
					value={form.newPassword}
					onChange={(event) =>
						setForm({ ...form, newPassword: event.target.value })
					}
					className="mt-4 w-full rounded-xl bg-black/30 p-3"
				/>
				<button
					onClick={submit}
					className="mt-5 rounded-full bg-white px-5 py-2 text-black"
				>
					Change
				</button>
				{message && <p className="mt-4">{message}</p>}
			</div>
		</main>
	);
}

export default function ChangePassword() {
	return (
		<AuthGate mode="authenticated">
			<ChangePasswordForm />
		</AuthGate>
	);
}
