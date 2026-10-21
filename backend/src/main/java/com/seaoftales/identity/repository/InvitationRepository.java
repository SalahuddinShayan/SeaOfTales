package com.seaoftales.identity.repository;
import com.seaoftales.identity.entity.Invitation; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import jakarta.persistence.LockModeType; import java.util.Optional;
public interface InvitationRepository extends JpaRepository<Invitation,Long>{
 Optional<Invitation> findByTokenHash(String tokenHash);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select i from Invitation i where i.id = :id")
 Optional<Invitation> findByIdForUpdate(@Param("id") Long id);
}
