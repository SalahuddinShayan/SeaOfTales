package com.seaoftales.knowledge.repository;

import com.seaoftales.knowledge.entity.TerminologyCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TerminologyCandidateRepository extends JpaRepository<TerminologyCandidate, Long> {
    List<TerminologyCandidate> findByChapterIdOrderByIdAsc(Long chapterId);
    Optional<TerminologyCandidate> findByChapterIdAndSourceTermAndTranslatedTerm(Long chapterId, String sourceTerm, String translatedTerm);
    boolean existsByChapterIdAndStatus(Long chapterId, com.seaoftales.knowledge.enums.TerminologyCandidateStatus status);
}
