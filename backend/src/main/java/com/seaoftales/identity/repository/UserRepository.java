package com.seaoftales.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seaoftales.identity.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository <User, Long> {
    java.util.Optional<User> findByUsername(String username);
    User findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    boolean existsByProfilePicture(String profilePicture);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM User user WHERE user.username = :username")
    java.util.Optional<User> findByUsernameForUpdate(@Param("username") String username);
}
