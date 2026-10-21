import Link from "next/link";
import NovelCover, { readableTitle } from "@/components/home/NovelCover";
import type { NovelSummary } from "@/components/home/home-types";

export default function NovelCard({
  novel,
  priority = false,
}: Readonly<{ novel: NovelSummary; priority?: boolean }>) {
  return (
    <article className="novels-card" key={novel.id}>
      <Link className="novels-card__cover" href={`/novels/${novel.id}`} aria-label={`Open ${readableTitle(novel.title)}`}>
        <NovelCover
          novel={novel}
          sizes="(max-width: 640px) 44vw, (max-width: 1024px) 30vw, 250px"
          priority={priority}
        />
      </Link>
      <div className="novels-card__body">
        <Link className="novels-card__title" href={`/novels/${novel.id}`} title={readableTitle(novel.title)}>
          {readableTitle(novel.title)}
        </Link>
        <div className="novels-card__meta">
          <span className="novels-card__status">{novel.status.toLowerCase().replaceAll("_", " ")}</span>
          <span>{novel.sourceLanguage.toLowerCase().replaceAll("_", " ")}</span>
        </div>
      </div>
    </article>
  );
}
