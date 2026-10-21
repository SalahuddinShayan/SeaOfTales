package com.seaoftales.content.repository;

import com.seaoftales.content.entity.ChapterSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChapterSourceRepository extends JpaRepository<ChapterSource, Long> {

    Optional<ChapterSource> findByChapterId(Long chapterId);
}
