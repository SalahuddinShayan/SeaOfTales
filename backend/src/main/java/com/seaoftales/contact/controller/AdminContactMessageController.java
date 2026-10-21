package com.seaoftales.contact.controller;

import com.seaoftales.contact.dto.ContactMessageDtos;
import com.seaoftales.contact.service.ContactMessageService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/contact-messages")
public class AdminContactMessageController {
    private final ContactMessageService service;
    public AdminContactMessageController(ContactMessageService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        try { return ResponseEntity.ok(service.list(page, size)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage())); }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try { return ResponseEntity.ok(service.get(id)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.notFound().build(); }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody ContactMessageDtos.StatusRequest request) {
        try { return ResponseEntity.ok(service.updateStatus(id, request.status())); }
        catch (IllegalArgumentException exception) {
            if ("Contact message not found.".equals(exception.getMessage())) return ResponseEntity.notFound().build();
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }
}
