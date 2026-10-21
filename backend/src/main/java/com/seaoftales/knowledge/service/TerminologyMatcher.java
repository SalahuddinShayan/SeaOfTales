package com.seaoftales.knowledge.service;

import com.seaoftales.knowledge.entity.Terminology;
import com.seaoftales.knowledge.enums.NameStyle;
import com.seaoftales.knowledge.enums.TerminologyType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TerminologyMatcher {

    public List<Terminology> match(String sourceText, List<Terminology> effectiveTerminology) {
        Map<Long, Terminology> matches = new LinkedHashMap<>();
        Map<String, Terminology> normalTerms = new LinkedHashMap<>();
        Map<String, List<Terminology>> characterTerms = new LinkedHashMap<>();

        for (Terminology terminology : effectiveTerminology) {
            if (terminology.getType() == TerminologyType.CHARACTER) {
                if (terminology.getNameStyle() == null) {
                    continue;
                }
                for (String component : characterComponents(terminology)) {
                    characterTerms.computeIfAbsent(component, ignored -> new ArrayList<>()).add(terminology);
                }
            } else {
                normalTerms.put(terminology.getSourceTerm(), terminology);
            }
        }

        for (Map.Entry<String, Terminology> entry : normalTerms.entrySet()) {
            if (!entry.getKey().isEmpty() && sourceText.contains(entry.getKey())) {
                matches.put(entry.getValue().getId(), entry.getValue());
            }
        }

        for (Map.Entry<String, List<Terminology>> entry : characterTerms.entrySet()) {
            if (sourceText.contains(entry.getKey()) && entry.getValue().size() == 1) {
                Terminology terminology = entry.getValue().get(0);
                matches.put(terminology.getId(), terminology);
            }
        }

        return new ArrayList<>(matches.values());
    }

    private List<String> characterComponents(Terminology terminology) {
        String sourceTerm = terminology.getSourceTerm();
        String[] parts = sourceTerm.trim().split("\\s+");
        if (terminology.getNameStyle() == NameStyle.KOREAN) {
            if (parts.length == 1 && parts[0].length() > 1) {
                return List.of(parts[0].substring(1));
            }
            if (parts.length == 1) {
                return List.of(parts[0]);
            }
            return List.of(parts[parts.length - 1]);
        }
        if (parts.length <= 1) {
            return List.of(sourceTerm.trim());
        }
        return List.of(parts[0], parts[parts.length - 1]);
    }
}
