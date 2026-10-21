export type NovelSummary = {
  id: number;
  title: string;
  status: string;
  sourceLanguage: string;
  description: string | null;
  coverImage: string | null;
};

export type ChapterSummary = {
  id: number;
  novelId: number;
  chapterNumber: string;
  title: string;
  updatedAt: string;
};

export type PopularNovel = {
  novel: NovelSummary;
  latestChapter: ChapterSummary | null;
};

export type NovelUpdate = {
  novel: NovelSummary;
  latestUpdatedAt: string;
  recentChapters: ChapterSummary[];
};

export type PublicHomeData = {
  featuredNovels: NovelSummary[];
  popularNovels: PopularNovel[];
  latestUpdates: NovelUpdate[];
};