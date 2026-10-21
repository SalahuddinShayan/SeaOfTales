package com.seaoftales.content.service;

import com.seaoftales.content.entity.*;
import com.seaoftales.content.repository.*;
import com.seaoftales.knowledge.entity.TerminologyCandidate;
import com.seaoftales.knowledge.entity.Terminology;
import com.seaoftales.knowledge.enums.TerminologyScope;
import com.seaoftales.knowledge.enums.NameStyle;
import com.seaoftales.knowledge.enums.TerminologyCandidateStatus;
import com.seaoftales.knowledge.repository.TerminologyCandidateRepository;
import com.seaoftales.knowledge.service.TerminologyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TranslationProcessingService {
    private final ChapterRepository chapterRepository;
    private final ChapterSourceRepository sourceRepository;
    private final ChapterTranslationRepository translationRepository;
    private final ChapterEditionRepository editionRepository;
    private final TerminologyCandidateRepository candidateRepository;
    private final TerminologyService terminologyService;
    private final NovelRepository novelRepository;
    private final TranslationFormatter formatter;
    private final TranslationResponseParser parser = new TranslationResponseParser();

    public TranslationProcessingService(ChapterRepository chapterRepository, ChapterSourceRepository sourceRepository,
                                        ChapterTranslationRepository translationRepository, ChapterEditionRepository editionRepository,
                                        TerminologyCandidateRepository candidateRepository, TerminologyService terminologyService,
                                        TranslationFormatter formatter, NovelRepository novelRepository) {
        this.chapterRepository = chapterRepository; this.sourceRepository = sourceRepository; this.translationRepository = translationRepository;
        this.editionRepository = editionRepository; this.candidateRepository = candidateRepository; this.terminologyService = terminologyService; this.formatter = formatter; this.novelRepository = novelRepository;
    }

    @Transactional
    public SubmissionResult submit(Long chapterId, String response) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new IllegalArgumentException("Chapter not found."));
        ChapterSource source = sourceRepository.findByChapterId(chapterId).orElseThrow(() -> new IllegalStateException("Chapter source not found."));
        if (translationRepository.findByChapterId(chapterId).isPresent()) {
            throw new IllegalStateException("A translation already exists for this chapter. Open it for editing.");
        }
        TranslationResponseParser.ParsedResponse parsed = parser.parse(response);
        ChapterTranslation translation = new ChapterTranslation(); translation.setChapterId(chapterId); translation.setTitle(parsed.title()); translation.setContent(parsed.content());
        List<String> issues = validate(source.getContent(), parsed.content());
        translation.setValidationStatus(issues.isEmpty() ? "VALID" : "WARNINGS");
        translation.setValidationIssues(String.join("\n", issues));
        ChapterTranslation saved = translationRepository.save(translation);
        List<TerminologyCandidate> candidates = processCandidates(chapter, parsed.newTerms());
        chapter.setState(com.seaoftales.content.enums.ChapterState.WAITING_EDIT); chapterRepository.save(chapter);
        return new SubmissionResult(saved, formatter.format(parsed.content()), issues, candidates);
    }

    public ChapterTranslation getTranslation(Long chapterId) {
        return translationRepository.findByChapterId(chapterId).orElseThrow(() -> new IllegalArgumentException("Translation not found."));
    }

    public ExistingTranslationResult getExistingTranslation(Long chapterId) {
        ChapterTranslation translation = getTranslation(chapterId);
        List<String> issues = translation.getValidationIssues() == null || translation.getValidationIssues().isBlank()
                ? List.of()
                : Arrays.stream(translation.getValidationIssues().split("\\R")).filter(s -> !s.isBlank()).toList();
        return new ExistingTranslationResult(translation, formatter.format(translation.getContent()), issues, candidates(chapterId));
    }

    private List<TerminologyCandidate> processCandidates(Chapter chapter, List<TranslationResponseParser.ParsedTerm> terms) {
        var novel = novelRepository.findById(chapter.getNovelId()).orElseThrow(() -> new IllegalStateException("Novel not found."));
        List<Terminology> effective = terminologyService.getEffective(novel.getSourceLanguage(), novel.getId());
        List<TerminologyCandidate> result = new ArrayList<>();
        for (TranslationResponseParser.ParsedTerm term : terms) {
            boolean known = effective.stream().anyMatch(existing -> existing.getSourceTerm().equals(term.sourceTerm()) && existing.getTranslatedTerm().equals(term.translatedTerm()));
            if (known) continue;
            TerminologyCandidate candidate = candidateRepository.findByChapterIdAndSourceTermAndTranslatedTerm(chapter.getId(), term.sourceTerm(), term.translatedTerm()).orElseGet(() -> {
                TerminologyCandidate created = new TerminologyCandidate(); created.setChapterId(chapter.getId()); created.setChapterNumber(chapter.getChapterNumber()); created.setSourceTerm(term.sourceTerm()); created.setTranslatedTerm(term.translatedTerm()); created.setType(term.type()); created.setGender(term.gender()); return created;
            });
            result.add(candidateRepository.save(candidate));
        }
        return result;
    }

    private List<String> validate(String source, String translation) {
        List<String> issues = new ArrayList<>();
        if (translation.trim().isEmpty()) issues.add("Translated content is empty.");
        if (translation.length() < Math.max(1, source.length() / 10)) issues.add("Translated content is suspiciously short compared with the source.");
        return issues;
    }

    public List<TerminologyCandidate> candidates(Long chapterId) { return candidateRepository.findByChapterIdOrderByIdAsc(chapterId); }

    @Transactional public TerminologyCandidate reject(Long candidateId) { return decide(candidateId, TerminologyCandidateStatus.REJECTED); }

    @Transactional
    public TerminologyCandidate approve(Long candidateId, TerminologyScope scope, Long novelId, com.seaoftales.content.enums.Language language, String sourceTerm, String translatedTerm, com.seaoftales.knowledge.enums.TerminologyType type, com.seaoftales.knowledge.enums.TerminologyGender gender, NameStyle nameStyle, String notes, boolean enabled) {
        TerminologyCandidate candidate = candidateRepository.findById(candidateId).orElseThrow(() -> new IllegalArgumentException("Terminology candidate not found."));
        candidate.setSourceTerm(sourceTerm); candidate.setTranslatedTerm(translatedTerm); candidate.setType(type); candidate.setGender(gender); candidate.setNameStyle(nameStyle); candidate.setNotes(notes);
        candidateRepository.save(candidate);
        Terminology terminology = new Terminology();
        terminology.setScope(scope); terminology.setNovelId(novelId); terminology.setLanguage(language);
        terminology.setSourceTerm(candidate.getSourceTerm()); terminology.setTranslatedTerm(candidate.getTranslatedTerm());
        terminology.setType(candidate.getType()); terminology.setGender(candidate.getGender()); terminology.setNameStyle(nameStyle); terminology.setNotes(notes); terminology.setEnabled(enabled);
        terminologyService.create(terminology);
        return decide(candidateId, TerminologyCandidateStatus.APPROVED);
    }

    @Transactional
    public ChapterEdition saveEdition(Long chapterId, String title, String formattedContent) {
        if (candidateRepository.existsByChapterIdAndStatus(chapterId, TerminologyCandidateStatus.UNREVIEWED)) {
            throw new IllegalStateException("All terminology candidates must be approved or rejected before saving the edition.");
        }
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new IllegalArgumentException("Chapter not found."));
        if (title == null || formattedContent == null || formattedContent.trim().isEmpty()) throw new IllegalArgumentException("Edition title and content are required.");
        ChapterEdition edition = editionRepository.findByChapterId(chapterId).orElseGet(ChapterEdition::new);
        edition.setChapterId(chapterId); edition.setTitle(title); edition.setContent(formattedContent);
        chapter.setState(com.seaoftales.content.enums.ChapterState.EDITED); chapterRepository.save(chapter);
        return editionRepository.save(edition);
    }

    private TerminologyCandidate decide(Long candidateId, TerminologyCandidateStatus status) {
        TerminologyCandidate candidate = candidateRepository.findById(candidateId).orElseThrow(() -> new IllegalArgumentException("Terminology candidate not found."));
        candidate.setStatus(status); candidate.setReviewedAt(Instant.now()); return candidateRepository.save(candidate);
    }

    public record SubmissionResult(ChapterTranslation translation, String formattedContent, List<String> validationIssues, List<TerminologyCandidate> candidates) { }
    public record ExistingTranslationResult(ChapterTranslation translation, String formattedContent, List<String> validationIssues, List<TerminologyCandidate> candidates) { }
}
