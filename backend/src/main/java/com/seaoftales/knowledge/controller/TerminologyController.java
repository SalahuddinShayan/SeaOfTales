package com.seaoftales.knowledge.controller;

import com.seaoftales.content.enums.Language;
import com.seaoftales.knowledge.entity.Terminology;
import com.seaoftales.knowledge.enums.TerminologyGender;
import com.seaoftales.knowledge.enums.TerminologyScope;
import com.seaoftales.knowledge.enums.TerminologyType;
import com.seaoftales.knowledge.enums.NameStyle;
import com.seaoftales.knowledge.service.TerminologyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminology")
public class TerminologyController {

    private final TerminologyService terminologyService;

    public TerminologyController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @GetMapping
    public ResponseEntity<List<Terminology>> getAll() {
        return ResponseEntity.ok(terminologyService.getAll());
    }

    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("scopes", TerminologyScope.values());
        options.put("languages", Language.values());
        options.put("types", TerminologyType.values());
        options.put("genders", TerminologyGender.values());
        options.put("nameStyles", NameStyle.values());
        return ResponseEntity.ok(options);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Terminology terminology) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(terminologyService.create(terminology));
        } catch (IllegalArgumentException exception) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("message", exception.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
