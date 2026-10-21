package com.seaoftales.knowledge.service;

import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.knowledge.entity.Terminology;
import com.seaoftales.knowledge.enums.TerminologyScope;
import com.seaoftales.knowledge.enums.TerminologyType;
import com.seaoftales.knowledge.repository.TerminologyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import com.seaoftales.content.enums.Language;

@Service
public class TerminologyService {

    private final TerminologyRepository terminologyRepository;
    private final NovelRepository novelRepository;

    public TerminologyService(
            TerminologyRepository terminologyRepository,
            NovelRepository novelRepository) {
        this.terminologyRepository = terminologyRepository;
        this.novelRepository = novelRepository;
    }

    public List<Terminology> getAll() {
        return terminologyRepository.findAll();
    }

    public List<Terminology> getEffective(Language language, Long novelId) {
        return terminologyRepository.findEffectiveByLanguageAndNovel(language, novelId);
    }

    public Terminology create(Terminology terminology) {
        if (terminology.getScope() == null) {
            throw new IllegalArgumentException("Scope is required.");
        }
        if (terminology.getLanguage() == null && terminology.getScope() == TerminologyScope.LANGUAGE) {
            throw new IllegalArgumentException("Language is required for LANGUAGE scope.");
        }
        if (terminology.getSourceTerm() == null || terminology.getSourceTerm().trim().isEmpty()) {
            throw new IllegalArgumentException("Source term is required.");
        }
        if (terminology.getTranslatedTerm() == null || terminology.getTranslatedTerm().trim().isEmpty()) {
            throw new IllegalArgumentException("Translated term is required.");
        }
        if (terminology.getType() == null) {
            throw new IllegalArgumentException("Type is required.");
        }
        if (terminology.getType() == TerminologyType.CHARACTER && terminology.getNameStyle() == null) {
            throw new IllegalArgumentException("Name style is required for CHARACTER terminology.");
        }
        if (terminology.getType() != TerminologyType.CHARACTER && terminology.getNameStyle() != null) {
            throw new IllegalArgumentException("Name style is only valid for CHARACTER terminology.");
        }

        if (terminology.getScope() == TerminologyScope.NOVEL) {
            if (terminology.getNovelId() == null) {
                throw new IllegalArgumentException("Novel is required for NOVEL scope.");
            }
            if (!novelRepository.existsById(terminology.getNovelId())) {
                throw new IllegalArgumentException("Selected novel does not exist.");
            }
        } else {
            terminology.setNovelId(null);
        }

        terminology.setSourceTerm(terminology.getSourceTerm().trim());
        terminology.setTranslatedTerm(terminology.getTranslatedTerm().trim());
        if (terminology.getNotes() != null) {
            terminology.setNotes(terminology.getNotes().trim());
        }

        return terminologyRepository.save(terminology);
    }
}
