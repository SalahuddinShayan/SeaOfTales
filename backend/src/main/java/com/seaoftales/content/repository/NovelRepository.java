package com.seaoftales.content.repository;

import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.enums.Language;
import com.seaoftales.content.enums.NovelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NovelRepository extends JpaRepository<Novel, Long>, JpaSpecificationExecutor<Novel> {
	List<Novel> findAllByOrderByCreatedAtAscIdAsc();

    // Group chapter timestamps per novel before pagination; globally sorted chapters would duplicate novels.
	@Query(value = """
            SELECT novel.id
            FROM Novel novel
            LEFT JOIN Chapter chapter ON chapter.novelId = novel.id
            LEFT JOIN ChapterSource source ON source.chapterId = chapter.id
            WHERE (:titleQuery IS NULL OR LOCATE(LOWER(:titleQuery), LOWER(novel.title)) > 0)
                AND (:language IS NULL OR novel.sourceLanguage = :language)
                AND (:status IS NULL OR novel.status = :status)
            GROUP BY novel.id
            ORDER BY CASE WHEN MAX(CASE WHEN source.id IS NOT NULL THEN chapter.updatedAt ELSE NULL END) IS NULL
                THEN 1 ELSE 0 END ASC,
                MAX(CASE WHEN source.id IS NOT NULL THEN chapter.updatedAt ELSE NULL END) DESC, novel.id ASC
            """,
        countQuery = """
            SELECT COUNT(novel)
            FROM Novel novel
            WHERE (:titleQuery IS NULL OR LOCATE(LOWER(:titleQuery), LOWER(novel.title)) > 0)
                AND (:language IS NULL OR novel.sourceLanguage = :language)
                AND (:status IS NULL OR novel.status = :status)
            """)
	Page<Long> findRecentlyUpdatedNovelIds(
			@Param("titleQuery") String titleQuery,
			@Param("language") Language language,
			@Param("status") NovelStatus status,
			Pageable pageable);
}