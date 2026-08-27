"use client";

import { useState } from "react";

export default function SeaOfTalesHome() {
  const [isNight, setIsNight] = useState(false);

  return (
    <main className="relative min-h-screen overflow-hidden bg-black">
      {/* Day background */}
      <div
        className={`absolute inset-0 bg-cover bg-center bg-no-repeat transition-opacity duration-1000 ease-in-out motion-reduce:transition-none ${
          isNight ? "opacity-0" : "opacity-100"
        }`}
        style={{ backgroundImage: "url('/images/sea-day.png')" }}
        aria-hidden="true"
      />

      {/* Night background */}
      <div
        className={`absolute inset-0 bg-cover bg-center bg-no-repeat transition-opacity duration-1000 ease-in-out motion-reduce:transition-none ${
          isNight ? "opacity-100" : "opacity-0"
        }`}
        style={{ backgroundImage: "url('/images/sea-night.png')" }}
        aria-hidden="true"
      />

      {/* Subtle overlay for navigation readability */}
      <div
        className="absolute inset-0 bg-linear-to-b from-black/25 via-transparent to-black/15"
        aria-hidden="true"
      />

      {/* Navigation */}
      <nav className="relative z-10 flex h-20 items-center justify-between px-6 sm:px-10 lg:px-16">
        <div
          className={`text-xl font-semibold tracking-[0.25em] transition-colors duration-700 sm:text-2xl ${
            isNight ? "text-white" : "text-white"
          }`}
        >
          SEA OF TALES
        </div>

        <button
          type="button"
          onClick={() => setIsNight((current) => !current)}
          className="flex h-11 w-11 items-center justify-center rounded-full border border-white/40 bg-black/10 text-xl text-white backdrop-blur-sm transition-all duration-300 hover:bg-white/20 focus:outline-none focus:ring-2 focus:ring-white/70 focus:ring-offset-2 focus:ring-offset-transparent"
          aria-label={isNight ? "Switch to daytime" : "Switch to nighttime"}
          title={isNight ? "Switch to day" : "Switch to night"}
        >
          <span aria-hidden="true">{isNight ? "☾" : "☀"}</span>
        </button>
      </nav>
    </main>
  );
}