package com.seaoftales.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seaoftales.identity.entity.User;

import java.util.List;

public interface PublicAuthorRepository extends JpaRepository<User, Long> {

    @Query(value = """
            WITH ranked_public_novels AS (
                SELECT n.id, n.author_username, n.title, n.status, n.source_language, n.description,
                       CASE WHEN n.cover_image IS NULL OR TRIM(n.cover_image) = ''
                            THEN NULL ELSE CONCAT('/api/novels/', n.id, '/cover') END AS cover_image,
                       COUNT(*) OVER (PARTITION BY n.author_username) AS author_novel_count,
                       ROW_NUMBER() OVER (PARTITION BY n.author_username ORDER BY n.id) AS author_novel_rank
                FROM novels n
            )
            SELECT u.username AS username, u.profile_picture AS profilePicture,
                   COALESCE(n.author_novel_count, 0) AS publicNovelCount,
                   n.id AS novelId, n.title AS title, n.status AS status,
                   n.source_language AS sourceLanguage, n.description AS description,
                   n.cover_image AS coverImage
            FROM users u
            LEFT JOIN ranked_public_novels n
                ON n.author_username = u.username AND n.author_novel_rank <= 3
            WHERE u.role = 'AUTHOR'
            ORDER BY LOWER(u.username), u.username, n.author_novel_rank
            """, nativeQuery = true)
    List<PublicAuthorRow> findPublicAuthors();

    @Query(value = """
            SELECT u.username AS username, u.profile_picture AS profilePicture,
                   n.id AS novelId, n.title AS title, n.status AS status,
                   n.source_language AS sourceLanguage, n.description AS description,
                   CASE WHEN n.cover_image IS NULL OR TRIM(n.cover_image) = ''
                        THEN NULL ELSE CONCAT('/api/novels/', n.id, '/cover') END AS coverImage
            FROM users u
            LEFT JOIN novels n ON n.author_username = u.username
            WHERE u.role = 'AUTHOR' AND LOWER(u.username) = LOWER(:username)
            ORDER BY n.id
            """, nativeQuery = true)
    List<PublicAuthorNovelRow> findPublicAuthor(@Param("username") String username);
}
