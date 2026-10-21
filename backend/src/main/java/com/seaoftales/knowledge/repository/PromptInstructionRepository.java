package com.seaoftales.knowledge.repository;

import com.seaoftales.knowledge.entity.PromptInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.seaoftales.content.enums.Language;
import java.util.List;

public interface PromptInstructionRepository extends JpaRepository<PromptInstruction, Long> {

    @Query("""
            SELECT p FROM PromptInstruction p
            WHERE p.enabled = true
              AND (p.scope = com.seaoftales.knowledge.enums.PromptInstructionScope.GLOBAL
                   OR (p.scope = com.seaoftales.knowledge.enums.PromptInstructionScope.LANGUAGE AND p.language = :language)
                   OR (p.scope = com.seaoftales.knowledge.enums.PromptInstructionScope.NOVEL AND p.novelId = :novelId))
            ORDER BY p.priority ASC, p.id ASC
            """)
    List<PromptInstruction> findEffective(@Param("language") Language language, @Param("novelId") Long novelId);
}
