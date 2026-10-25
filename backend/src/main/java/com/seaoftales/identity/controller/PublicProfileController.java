package com.seaoftales.identity.controller;

import com.seaoftales.identity.dto.PublicProfileResponse;
import com.seaoftales.identity.dto.ProfileActivityComment;
import com.seaoftales.identity.dto.ProfileActivityPage;
import com.seaoftales.identity.dto.ProfileActivityReaction;
import com.seaoftales.identity.dto.UpdateProfileRequest;
import com.seaoftales.identity.service.PublicProfileActivityService;
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
    private final PublicProfileService profiles; private final PublicProfileActivityService activity; private final ImageStorageService storage;

    public PublicProfileController(PublicProfileService profiles, PublicProfileActivityService activity, ImageStorageService storage) { this.profiles = profiles; this.activity = activity; this.storage = storage; }

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

    @GetMapping("/{username}/activity/comments")
    public ResponseEntity<ProfileActivityPage<ProfileActivityComment>> comments(
            @PathVariable String username, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validPage(page, size)) return ResponseEntity.badRequest().build();
        try { return ResponseEntity.ok(activity.comments(username, page, size)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{username}/activity/reactions")
    public ResponseEntity<ProfileActivityPage<ProfileActivityReaction>> reactions(
            @PathVariable String username, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!validPage(page, size)) return ResponseEntity.badRequest().build();
        try { return ResponseEntity.ok(activity.reactions(username, page, size)); }
        catch (IllegalArgumentException exception) { return ResponseEntity.notFound().build(); }
    }

    private boolean validPage(int page, int size) { return page >= 0 && size >= 1 && size <= 50; }

    @PatchMapping("/me")
    public ResponseEntity<?> update(@RequestBody UpdateProfileRequest ignored, Authentication authentication) {
        return ResponseEntity.status(422).body(java.util.Map.of("message", "There are no editable profile fields available yet."));
    }
}
