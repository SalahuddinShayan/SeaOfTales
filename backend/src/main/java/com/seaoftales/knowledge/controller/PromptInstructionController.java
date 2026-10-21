package com.seaoftales.knowledge.controller;

import com.seaoftales.knowledge.entity.PromptInstruction;
import com.seaoftales.knowledge.service.PromptInstructionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompt-instructions")
public class PromptInstructionController {

    private final PromptInstructionService promptInstructionService;

    public PromptInstructionController(PromptInstructionService promptInstructionService) {
        this.promptInstructionService = promptInstructionService;
    }

    @GetMapping
    public ResponseEntity<List<PromptInstruction>> getAll() {
        return ResponseEntity.ok(promptInstructionService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PromptInstruction promptInstruction) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(promptInstructionService.create(promptInstruction));
        } catch (IllegalArgumentException exception) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("message", exception.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
