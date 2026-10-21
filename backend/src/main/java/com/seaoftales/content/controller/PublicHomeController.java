package com.seaoftales.content.controller;

import com.seaoftales.content.dto.PublicHomeResponse;
import com.seaoftales.content.service.PublicHomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class PublicHomeController {

    private final PublicHomeService publicHomeService;

    public PublicHomeController(PublicHomeService publicHomeService) {
        this.publicHomeService = publicHomeService;
    }

    @GetMapping
    public ResponseEntity<PublicHomeResponse> getHome() {
        return ResponseEntity.ok(publicHomeService.getHome());
    }
}