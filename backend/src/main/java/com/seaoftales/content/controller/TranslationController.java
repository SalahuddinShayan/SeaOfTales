package com.seaoftales.content.controller;

import com.seaoftales.content.service.TranslationProcessingService;
import com.seaoftales.knowledge.enums.NameStyle;
import com.seaoftales.knowledge.enums.TerminologyScope;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chapters")
public class TranslationController {
    private final TranslationProcessingService service;
    public TranslationController(TranslationProcessingService service) { this.service = service; }

    @PostMapping("/{chapterId}/translation")
    public ResponseEntity<?> submit(@PathVariable Long chapterId, @RequestBody Map<String, String> request) {
        try {
            var result = service.submit(chapterId, request.get("response"));
            Map<String, Object> body = new LinkedHashMap<>(); body.put("translation", result.translation()); body.put("formattedContent", result.formattedContent()); body.put("validationIssues", result.validationIssues()); body.put("candidates", result.candidates());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalArgumentException e) { return error(HttpStatus.BAD_REQUEST, e.getMessage()); }
        catch (IllegalStateException e) { return error(HttpStatus.NOT_FOUND, e.getMessage()); }
    }

    @GetMapping("/{chapterId}/translation")
    public ResponseEntity<?> getTranslation(@PathVariable Long chapterId) {
        try {
            var result = service.getExistingTranslation(chapterId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("translation", result.translation());
            body.put("formattedContent", result.formattedContent());
            body.put("validationIssues", result.validationIssues());
            body.put("candidates", result.candidates());
            return ResponseEntity.ok(body);
        }
        catch (IllegalArgumentException e) { return error(HttpStatus.NOT_FOUND, e.getMessage()); }
    }

    @GetMapping("/{chapterId}/candidates")
    public ResponseEntity<?> candidates(@PathVariable Long chapterId) { return ResponseEntity.ok(service.candidates(chapterId)); }

    @PostMapping("/candidates/{candidateId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long candidateId) { try { return ResponseEntity.ok(service.reject(candidateId)); } catch (IllegalArgumentException e) { return error(HttpStatus.NOT_FOUND, e.getMessage()); } }

    @PostMapping("/candidates/{candidateId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long candidateId, @RequestBody ApprovalRequest request) {
        try { return ResponseEntity.ok(service.approve(candidateId, request.scope(), request.novelId(), request.language(), request.sourceTerm(), request.translatedTerm(), request.type(), request.gender(), request.nameStyle(), request.notes(), request.enabled())); }
        catch (IllegalArgumentException e) { return error(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }

    @PostMapping("/{chapterId}/edition")
    public ResponseEntity<?> edition(@PathVariable Long chapterId, @RequestBody EditionRequest request) {
        try { return ResponseEntity.ok(service.saveEdition(chapterId, request.title(), request.formattedContent())); }
        catch (IllegalArgumentException e) { return error(HttpStatus.BAD_REQUEST, e.getMessage()); }
        catch (IllegalStateException e) { return error(HttpStatus.CONFLICT, e.getMessage()); }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) { Map<String, String> body = new LinkedHashMap<>(); body.put("message", message); return ResponseEntity.status(status).body(body); }
    public record ApprovalRequest(TerminologyScope scope, Long novelId, com.seaoftales.content.enums.Language language, String sourceTerm, String translatedTerm, com.seaoftales.knowledge.enums.TerminologyType type, com.seaoftales.knowledge.enums.TerminologyGender gender, NameStyle nameStyle, String notes, boolean enabled) { }
    public record EditionRequest(String title, String formattedContent) { }
}
