package com.seaoftales.content.service;

import com.seaoftales.knowledge.enums.TerminologyGender;
import com.seaoftales.knowledge.enums.TerminologyType;

import java.util.ArrayList;
import java.util.List;

public class TranslationResponseParser {
    public static final String TITLE_START = "<<<SOT_TITLE>>>";
    public static final String TITLE_END = "<<<END_SOT_TITLE>>>";
    public static final String CONTENT_START = "<<<SOT_CONTENT>>>";
    public static final String CONTENT_END = "<<<END_SOT_CONTENT>>>";
    public static final String TERMS_START = "<<<SOT_NEW_TERMS>>>";
    public static final String TERMS_END = "<<<END_SOT_NEW_TERMS>>>";

    public ParsedResponse parse(String response) {
        if (response == null) throw new IllegalArgumentException("AI response is required.");
        String title = section(response, TITLE_START, TITLE_END);
        String content = section(response, CONTENT_START, CONTENT_END);
        String terms = section(response, TERMS_START, TERMS_END);
        if (content.trim().isEmpty()) throw new IllegalArgumentException("Translated content is required.");
        List<ParsedTerm> parsedTerms = new ArrayList<>();
        for (String line : terms.split("\\R")) {
            if (line.trim().isEmpty()) continue;
            parsedTerms.add(parseTerm(line));
        }
        return new ParsedResponse(title, content, parsedTerms);
    }

    private String section(String response, String start, String end) {
        int first = response.indexOf(start);
        int close = response.indexOf(end);
        if (first < 0 || close < 0 || close < first + start.length() || response.indexOf(start, first + start.length()) >= 0 || response.indexOf(end, close + end.length()) >= 0) {
            throw new IllegalArgumentException("Malformed or missing response section: " + start);
        }
        return response.substring(first + start.length(), close).replaceFirst("^\\r?\\n", "").replaceFirst("\\r?\\n$", "");
    }

    private ParsedTerm parseTerm(String line) {
        String[] arrow = line.split("→", 2);
        if (arrow.length != 2) throw new IllegalArgumentException("Malformed new-term line: " + line);
        String[] fields = arrow[1].trim().split("\\s+-\\s+", -1);
        if (fields.length < 2 || fields.length > 3 || arrow[0].trim().isEmpty() || fields[0].trim().isEmpty()) throw new IllegalArgumentException("Malformed new-term line: " + line);
        TerminologyType type;
        try { type = TerminologyType.valueOf(fields[1].trim()); } catch (Exception e) { throw new IllegalArgumentException("Invalid terminology type: " + fields[1].trim()); }
        TerminologyGender gender = null;
        if (type == TerminologyType.CHARACTER) {
            if (fields.length != 3) throw new IllegalArgumentException("Character term requires gender: " + line);
            try { gender = TerminologyGender.valueOf(fields[2].trim()); } catch (Exception e) { throw new IllegalArgumentException("Invalid terminology gender: " + fields[2].trim()); }
        } else if (fields.length == 3) {
            throw new IllegalArgumentException("Non-character term must not include gender: " + line);
        }
        return new ParsedTerm(arrow[0].trim(), fields[0].trim(), type, gender);
    }

    public record ParsedResponse(String title, String content, List<ParsedTerm> newTerms) { }
    public record ParsedTerm(String sourceTerm, String translatedTerm, TerminologyType type, TerminologyGender gender) { }
}
