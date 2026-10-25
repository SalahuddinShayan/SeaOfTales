package com.seaoftales.identity.service;

import com.seaoftales.identity.dto.PublicProfileResponse;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.identity.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

@Service
public class PublicProfileService {
    private final UserRepository users;

    public PublicProfileService(UserRepository users) { this.users = users; }

    public PublicProfileResponse getProfile(String username) {
        User user = requirePublicProfileOwner(username);
        return new PublicProfileResponse(user.getUsername(), user.getRole().name(),
                user.getProfilePicture() == null ? null : "/api/profiles/" + user.getUsername() + "/picture");
    }

    public User requirePublicProfileOwner(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Profile not found.");
        return users.findByUsername(username.trim())
                .filter(user -> user.getRole() != com.seaoftales.identity.enums.UserRole.AUTHOR)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found."));
    }

    public Resource getPicture(String username, com.seaoftales.storage.ImageStorageService storage) {
        var user = users.findByUsername(username == null ? "" : username.trim()).orElseThrow(() -> new IllegalArgumentException("Profile not found."));
        return storage.load(user.getProfilePicture());
    }
}
