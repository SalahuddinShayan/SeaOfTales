package com.seaoftales.reaction.controller;

import com.seaoftales.reaction.dto.ReactionSummary;
import com.seaoftales.reaction.dto.SetReactionRequest;
import com.seaoftales.reaction.enums.ReactionTargetType;
import com.seaoftales.reaction.service.ReactionService;
import com.seaoftales.reaction.service.ReactionService.TargetNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/reactions")
public class ReactionController {
    private final ReactionService service;

    public ReactionController(ReactionService service) { this.service = service; }

    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<?> summary(@PathVariable String targetType, @PathVariable Long targetId,
                                     Authentication authentication) {
        try {
            return ResponseEntity.ok(service.summary(parseTargetType(targetType), targetId, authenticatedName(authentication)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (TargetNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Reaction target not found."));
        }
    }

    @PutMapping("/{targetType}/{targetId}")
    public ResponseEntity<?> toggle(@PathVariable String targetType, @PathVariable Long targetId,
                                    @Valid @RequestBody SetReactionRequest request,
                                    Authentication authentication) {
        try {
            return ResponseEntity.ok(service.toggle(authentication.getName(), parseTargetType(targetType), targetId,
                    request.reactionType()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (TargetNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Reaction target not found."));
        }
    }

    private ReactionTargetType parseTargetType(String targetType) {
        try {
            return ReactionTargetType.valueOf(targetType.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported reaction target type.");
        }
    }

    private String authenticatedName(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken ? null : authentication.getName();
    }
}
