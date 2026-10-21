package com.seaoftales.content.repository;

import com.seaoftales.content.entity.ChapterTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterTranslationRepository extends JpaRepository<ChapterTranslation, Long> {
    java.util.Optional<ChapterTranslation> findByChapterId(Long chapterId);
}
