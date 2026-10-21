package com.seaoftales.content.repository;

import com.seaoftales.content.entity.Chapter;
import com.seaoftales.content.dto.HomeChapterRow;
import com.seaoftales.content.dto.PublicUpdateRow;
import com.seaoftales.content.dto.PublicNovelChapterRow;
import com.seaoftales.content.dto.PublicChapterReaderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    @Query("""
            SELECT new com.seaoftales.content.dto.PublicUpdateRow(
                c.id, c.novelId, n.title, n.coverImage, c.chapterNumber,
                CASE WHEN c.state = com.seaoftales.content.enums.ChapterState.EDITED
                    AND edition.title IS NOT NULL AND edition.title <> ''
                    THEN edition.title ELSE source.title END,
                c.updatedAt)
            FROM Chapter c
            JOIN Novel n ON n.id = c.novelId
            JOIN ChapterSource source ON source.chapterId = c.id
            LEFT JOIN ChapterEdition edition ON edition.chapterId = c.id
            ORDER BY c.updatedAt DESC, c.id DESC
            """)
    Page<PublicUpdateRow> findPublicUpdates(Pageable pageable);

    boolean existsByNovelIdAndChapterNumber(Long novelId, String chapterNumber);

    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Long novelId);

    @Query("""
            SELECT new com.seaoftales.content.dto.PublicNovelChapterRow(
                c.id, c.novelId, c.chapterNumber, edition.title, c.updatedAt)
            FROM Chapter c
            JOIN ChapterEdition edition ON edition.chapterId = c.id
            WHERE c.novelId = :novelId
                AND c.state = com.seaoftales.content.enums.ChapterState.EDITED
            ORDER BY c.chapterNumber ASC
            """)
    List<PublicNovelChapterRow> findPublicChaptersByNovelId(@Param("novelId") Long novelId);

    @Query("""
            SELECT new com.seaoftales.content.dto.PublicChapterReaderRow(
                c.id, c.novelId, c.chapterNumber, edition.title, edition.content, c.updatedAt)
            FROM Chapter c
            JOIN ChapterEdition edition ON edition.chapterId = c.id
            WHERE c.novelId = :novelId
                AND c.state = com.seaoftales.content.enums.ChapterState.EDITED
            ORDER BY c.chapterNumber ASC
            """)
    List<PublicChapterReaderRow> findPublicChapterReaderRowsByNovelId(@Param("novelId") Long novelId);

    @Query("""
            SELECT c.novelId AS novelId, MAX(c.updatedAt) AS latestUpdatedAt
            FROM Chapter c JOIN ChapterSource source ON source.chapterId = c.id
            GROUP BY c.novelId
            ORDER BY MAX(c.updatedAt) DESC, c.novelId ASC
            """)
    List<LatestNovelUpdate> findLatestNovelUpdates();

    @Query(value = """
            SELECT c.novelId
            FROM Chapter c JOIN ChapterSource source ON source.chapterId = c.id
            GROUP BY c.novelId
            ORDER BY MAX(c.updatedAt) DESC, c.novelId ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT c.novelId)
            FROM Chapter c JOIN ChapterSource source ON source.chapterId = c.id
            """)
    Page<Long> findLatestNovelUpdateIds(Pageable pageable);

    @Query("""
            SELECT new com.seaoftales.content.dto.HomeChapterRow(
                c.id,
                c.novelId,
                c.chapterNumber,
                CASE WHEN c.state = com.seaoftales.content.enums.ChapterState.EDITED
                    AND edition.title IS NOT NULL AND edition.title <> ''
                    THEN edition.title ELSE source.title END,
                c.updatedAt
            )
            FROM Chapter c
            JOIN ChapterSource source ON source.chapterId = c.id
            LEFT JOIN ChapterEdition edition ON edition.chapterId = c.id
            WHERE c.novelId = :novelId
            ORDER BY c.updatedAt DESC, c.id DESC
            """)
    List<HomeChapterRow> findHomeChaptersByNovelId(@Param("novelId") Long novelId, Pageable pageable);

    interface LatestNovelUpdate {
        Long getNovelId();

        Instant getLatestUpdatedAt();
    }
}
