package com.seaoftales.knowledge.service;

import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.knowledge.entity.PromptInstruction;
import com.seaoftales.knowledge.enums.PromptInstructionScope;
import com.seaoftales.knowledge.repository.PromptInstructionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import com.seaoftales.content.enums.Language;

@Service
public class PromptInstructionService {

    private final PromptInstructionRepository promptInstructionRepository;
    private final NovelRepository novelRepository;

    public PromptInstructionService(
            PromptInstructionRepository promptInstructionRepository,
            NovelRepository novelRepository) {
        this.promptInstructionRepository = promptInstructionRepository;
        this.novelRepository = novelRepository;
    }

    public List<PromptInstruction> getAll() {
        return promptInstructionRepository.findAll();
    }

    public List<PromptInstruction> getEffective(Language language, Long novelId) {
        return promptInstructionRepository.findEffective(language, novelId);
    }

    public PromptInstruction create(PromptInstruction promptInstruction) {
        if (promptInstruction.getScope() == null) {
            throw new IllegalArgumentException("Scope is required.");
        }

        if (promptInstruction.getInstruction() == null
                || promptInstruction.getInstruction().trim().isEmpty()) {
            throw new IllegalArgumentException("Instruction is required.");
        }

        if (promptInstruction.getPriority() == null || promptInstruction.getPriority() < 0) {
            throw new IllegalArgumentException("Priority must be zero or greater.");
        }

        if (promptInstruction.getScope() == PromptInstructionScope.LANGUAGE
                && promptInstruction.getLanguage() == null) {
            throw new IllegalArgumentException("Language is required for LANGUAGE scope.");
        }

        if (promptInstruction.getScope() == PromptInstructionScope.NOVEL) {
            if (promptInstruction.getNovelId() == null) {
                throw new IllegalArgumentException("Novel is required for NOVEL scope.");
            }
            if (!novelRepository.existsById(promptInstruction.getNovelId())) {
                throw new IllegalArgumentException("Selected novel does not exist.");
            }
        } else {
            promptInstruction.setNovelId(null);
        }

        if (promptInstruction.getScope() == PromptInstructionScope.GLOBAL) {
            promptInstruction.setLanguage(null);
        }

        promptInstruction.setInstruction(promptInstruction.getInstruction().trim());
        return promptInstructionRepository.save(promptInstruction);
    }
}
