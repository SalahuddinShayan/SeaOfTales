"use client";

import Image from "next/image";
import { useState } from "react";
import type { NovelSummary } from "@/components/home/home-types";
import { useSiteTheme } from "@/components/site/SiteThemeProvider";

type NovelCoverProps = { novel: NovelSummary; sizes: string; priority?: boolean };

export function readableTitle(title: string) {
  return title.replace(/<[^>]*>/g, "").trim();
}

export default function NovelCover({ novel, sizes, priority = false }: NovelCoverProps) {
  const { theme } = useSiteTheme();
  const [failedCover, setFailedCover] = useState("");
  const hasStoredCover = Boolean(novel.coverImage && failedCover !== novel.coverImage);
  const source = hasStoredCover
    ? novel.coverImage!
    : theme === "night" ? "/images/sea-night.png" : "/images/sea-day.png";

  return (
    <div className={`novel-cover ${hasStoredCover ? "novel-cover--image" : "novel-cover--placeholder"}`}>
      <Image
        src={source}
        alt={`Cover art for ${readableTitle(novel.title)}`}
        fill
        sizes={sizes}
        priority={priority}
        onError={hasStoredCover ? () => setFailedCover(novel.coverImage ?? "") : undefined}
        className="novel-cover__image"
      />
      {!hasStoredCover && (
        <div className="novel-cover__caption">
          <span>SEA OF TALES</span>
          <strong>{readableTitle(novel.title)}</strong>
        </div>
      )}
    </div>
  );
}