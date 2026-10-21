package com.seaoftales.contact.controller;

import com.seaoftales.contact.dto.ContactMessageDtos;
import com.seaoftales.contact.service.ContactMessageService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactMessageService service;
    public ContactController(ContactMessageService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody ContactMessageDtos.PublicRequest request) {
        try { return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(request)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage())); }
        catch (RuntimeException exception) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "The enquiry could not be saved right now.")); }
    }
}
