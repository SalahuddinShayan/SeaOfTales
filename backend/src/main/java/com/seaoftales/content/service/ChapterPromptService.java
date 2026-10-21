package com.seaoftales.content.service;

import com.seaoftales.content.entity.Chapter;
import com.seaoftales.content.entity.ChapterSource;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.ChapterRepository;
import com.seaoftales.content.repository.ChapterSourceRepository;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.knowledge.entity.PromptInstruction;
import com.seaoftales.knowledge.entity.Terminology;
import com.seaoftales.knowledge.service.PromptInstructionService;
import com.seaoftales.knowledge.service.TerminologyMatcher;
import com.seaoftales.knowledge.service.TerminologyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChapterPromptService {

    private final ChapterRepository chapterRepository;
    private final ChapterSourceRepository chapterSourceRepository;
    private final NovelRepository novelRepository;
    private final TerminologyService terminologyService;
    private final TerminologyMatcher terminologyMatcher;
    private final PromptInstructionService promptInstructionService;
    private final PromptBuilder promptBuilder;

    public ChapterPromptService(
            ChapterRepository chapterRepository,
            ChapterSourceRepository chapterSourceRepository,
            NovelRepository novelRepository,
            TerminologyService terminologyService,
            TerminologyMatcher terminologyMatcher,
            PromptInstructionService promptInstructionService,
            PromptBuilder promptBuilder) {
        this.chapterRepository = chapterRepository;
        this.chapterSourceRepository = chapterSourceRepository;
        this.novelRepository = novelRepository;
        this.terminologyService = terminologyService;
        this.terminologyMatcher = terminologyMatcher;
        this.promptInstructionService = promptInstructionService;
        this.promptBuilder = promptBuilder;
    }

    public PromptBuildResult buildPrompt(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found."));
        ChapterSource source = chapterSourceRepository.findByChapterId(chapterId)
                .orElseThrow(() -> new IllegalStateException("Chapter source not found."));
        Novel novel = novelRepository.findById(chapter.getNovelId())
                .orElseThrow(() -> new IllegalStateException("Novel not found."));

        List<Terminology> effectiveTerminology = terminologyService.getEffective(novel.getSourceLanguage(), novel.getId());
        List<Terminology> matchedTerminology = terminologyMatcher.match(source.getContent(), effectiveTerminology);
        List<PromptInstruction> instructions = promptInstructionService.getEffective(novel.getSourceLanguage(), novel.getId());
        String prompt = promptBuilder.build(source, instructions, matchedTerminology);

        return new PromptBuildResult(chapter, source, prompt);
    }

    public record PromptBuildResult(Chapter chapter, ChapterSource source, String prompt) {
    }
}
