package com.seaoftales.identity.controller;

import com.seaoftales.identity.dto.PublicAuthorResponse;
import com.seaoftales.identity.service.PublicAuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
public class PublicAuthorController {
    private final PublicAuthorService authors;

    public PublicAuthorController(PublicAuthorService authors) {
        this.authors = authors;
    }

    @GetMapping
    public List<PublicAuthorResponse> getAuthors() {
        return authors.getAuthors();
    }

    @GetMapping("/{username}")
    public ResponseEntity<PublicAuthorResponse> getAuthor(@PathVariable String username) {
        try {
            return ResponseEntity.ok(authors.getAuthor(username));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
