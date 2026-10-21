package com.seaoftales.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seaoftales.identity.entity.User;

public interface UserRepository extends JpaRepository <User, Long> {
    java.util.Optional<User> findByUsername(String username);
    User findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    boolean existsByProfilePicture(String profilePicture);
}
