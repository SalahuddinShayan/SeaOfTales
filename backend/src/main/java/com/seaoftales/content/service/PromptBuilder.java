package com.seaoftales.content.service;

import com.seaoftales.content.entity.ChapterSource;
import com.seaoftales.knowledge.entity.PromptInstruction;
import com.seaoftales.knowledge.entity.Terminology;
import com.seaoftales.knowledge.enums.TerminologyGender;
import com.seaoftales.knowledge.enums.TerminologyType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromptBuilder {

    public String build(
            ChapterSource source,
            List<PromptInstruction> instructions,
            List<Terminology> terminology) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[TRANSLATION INSTRUCTIONS]\n\n");
        for (PromptInstruction instruction : instructions) {
            prompt.append(instruction.getInstruction()).append("\n\n");
        }

        List<Terminology> normalTerminology = new ArrayList<>();
        List<Terminology> maleCharacters = new ArrayList<>();
        List<Terminology> femaleCharacters = new ArrayList<>();
        List<Terminology> unknownGenderCharacters = new ArrayList<>();
        List<Terminology> unclassifiedCharacters = new ArrayList<>();

        for (Terminology entry : terminology) {
            if (entry.getType() != TerminologyType.CHARACTER) {
                normalTerminology.add(entry);
                continue;
            }

            if (entry.getGender() == TerminologyGender.MALE) {
                maleCharacters.add(entry);
            } else if (entry.getGender() == TerminologyGender.FEMALE) {
                femaleCharacters.add(entry);
            } else if (entry.getGender() == TerminologyGender.UNKNOWN || entry.getGender() == null) {
                unknownGenderCharacters.add(entry);
            } else {
                // Preserve invalid/inconsistent NOT_APPLICABLE character data without guessing.
                unclassifiedCharacters.add(entry);
            }
        }

        appendMappingSection(prompt, "[TERMINOLOGY]", normalTerminology);
        appendMappingSection(prompt, "[CHARACTERS — MALE]", maleCharacters);
        appendMappingSection(prompt, "[CHARACTERS — FEMALE]", femaleCharacters);
        appendMappingSection(prompt, "[CHARACTERS — GENDER NOT CONFIRMED]", unknownGenderCharacters);
        appendMappingSection(prompt, "[CHARACTERS — UNCLASSIFIED]", unclassifiedCharacters);

        prompt.append("\n[CHAPTER]\n\n");
        prompt.append("Title: ").append(source.getTitle()).append("\n\n");
        prompt.append("Content:\n");
        prompt.append(source.getContent());
        prompt.append("\n\n[RESPONSE FORMAT — REQUIRED]\n\n")
                .append("Return exactly these three sections using the fixed markers below. Do not add HTML formatting.\n\n")
                .append(TranslationResponseParser.TITLE_START).append("\n")
                .append("Translated chapter title, if one exists.\n")
                .append(TranslationResponseParser.TITLE_END).append("\n\n")
                .append(TranslationResponseParser.CONTENT_START).append("\n")
                .append("Complete translated chapter content.\n")
                .append(TranslationResponseParser.CONTENT_END).append("\n\n")
                .append(TranslationResponseParser.TERMS_START).append("\n")
                .append("Place new terms identified according to the translation instructions here.\n")
                .append("Valid terminology types are: CHARACTER, PLACE, ORGANIZATION, TITLE, TECHNIQUE, SPECIES, OBJECT, OTHER.\n")
                .append("For non-CHARACTER terms use source → translation - type. For CHARACTER terms use source → translation - CHARACTER - gender.\n")
                .append("Gender must be MALE, FEMALE, or UNKNOWN; always provide it for CHARACTER terms and never guess.\n")
                .append("Do not include nameStyle, notes, scope, or other database fields. Leave this section empty when there are no new terms.\n")
                .append(TranslationResponseParser.TERMS_END);
        return prompt.toString();
    }

    private void appendMappingSection(StringBuilder prompt, String heading, List<Terminology> entries) {
        if (entries.isEmpty()) {
            return;
        }

        prompt.append(heading).append("\n\n");
        for (Terminology entry : entries) {
            prompt.append(entry.getSourceTerm())
                    .append(" → ")
                    .append(entry.getTranslatedTerm())
                    .append("\n");
        }
        prompt.append("\n");
    }
}
