package com.seaoftales.identity.repository;
import com.seaoftales.identity.entity.PendingRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.*;
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingRegistration> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);
    boolean existsByProfilePictureAndConsumedFalse(String profilePicture);
}
