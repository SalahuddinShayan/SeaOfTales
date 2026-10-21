package com.seaoftales.content.controller;

import com.seaoftales.content.dto.PublicUpdatesResponse;
import com.seaoftales.content.service.PublicUpdatesService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/updates")
public class PublicUpdatesController {
    private final PublicUpdatesService service;
    public PublicUpdatesController(PublicUpdatesService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> getUpdates(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        try { return ResponseEntity.ok(service.getUpdates(page, size)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().body(java.util.Map.of("message", exception.getMessage())); }
    }
}
