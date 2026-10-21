package com.seaoftales.content.repository;

import com.seaoftales.content.entity.ChapterEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChapterEditionRepository extends JpaRepository<ChapterEdition, Long> {
    Optional<ChapterEdition> findByChapterId(Long chapterId);
}
