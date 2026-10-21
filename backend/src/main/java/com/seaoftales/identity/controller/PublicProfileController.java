package com.seaoftales.identity.controller;

import com.seaoftales.identity.dto.PublicProfileResponse;
import com.seaoftales.identity.dto.UpdateProfileRequest;
import com.seaoftales.identity.service.PublicProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import com.seaoftales.storage.ImageStorageService;

@RestController
@RequestMapping("/api/profiles")
public class PublicProfileController {
    private final PublicProfileService profiles; private final ImageStorageService storage;

    public PublicProfileController(PublicProfileService profiles, ImageStorageService storage) { this.profiles = profiles; this.storage = storage; }

    @GetMapping("/{username}")
    public ResponseEntity<PublicProfileResponse> get(@PathVariable String username) {
        try { return ResponseEntity.ok(profiles.getProfile(username)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{username}/picture")
    public ResponseEntity<Resource> picture(@PathVariable String username) {
        try { Resource image = profiles.getPicture(username, storage); return ResponseEntity.ok().contentType(MediaTypeFactory.getMediaType(image).orElse(MediaType.APPLICATION_OCTET_STREAM)).body(image); }
        catch (IllegalArgumentException exception) { return ResponseEntity.notFound().build(); }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> update(@RequestBody UpdateProfileRequest ignored, Authentication authentication) {
        return ResponseEntity.status(422).body(java.util.Map.of("message", "There are no editable profile fields available yet."));
    }
}
