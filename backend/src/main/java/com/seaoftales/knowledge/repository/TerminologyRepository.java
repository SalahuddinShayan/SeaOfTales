package com.seaoftales.knowledge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.seaoftales.content.enums.Language;
import java.util.List;

import com.seaoftales.knowledge.entity.Terminology;

public interface TerminologyRepository extends JpaRepository<Terminology, Long> {

    @Query("""
            SELECT t FROM Terminology t
            WHERE t.enabled = true
              AND ((t.scope = com.seaoftales.knowledge.enums.TerminologyScope.LANGUAGE AND t.language = :language)
                   OR (t.scope = com.seaoftales.knowledge.enums.TerminologyScope.NOVEL AND t.novelId = :novelId))
              AND NOT EXISTS (
                    SELECT overrideEntry.id FROM Terminology overrideEntry
                    WHERE overrideEntry.enabled = true
                      AND overrideEntry.scope = com.seaoftales.knowledge.enums.TerminologyScope.NOVEL
                      AND overrideEntry.novelId = :novelId
                      AND overrideEntry.sourceTerm = t.sourceTerm
                      AND t.scope = com.seaoftales.knowledge.enums.TerminologyScope.LANGUAGE
              )
            ORDER BY t.id
            """)
    List<Terminology> findEffectiveByLanguageAndNovel(
            @Param("language") Language language,
            @Param("novelId") Long novelId);

}
