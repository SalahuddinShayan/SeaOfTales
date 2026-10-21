package com.seaoftales.identity.entity;

import com.seaoftales.content.enums.Language;
import com.seaoftales.identity.enums.PendingRegistrationType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name="pending_registrations", indexes={
        @Index(name="ix_pending_email", columnList="email"),
        @Index(name="ix_pending_invitation", columnList="invitationId")})
public class PendingRegistration {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=50) private String username;
    @Column(nullable=false,length=255) private String email;
    @Column(nullable=false,length=255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PendingRegistrationType type;
    @Enumerated(EnumType.STRING) @Column(length=20) private Language preferredLanguage;
    @Column(length=500) private String profilePicture;
    private Long invitationId;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean consumed;
    @CreationTimestamp @Column(nullable=false,updatable=false) private Instant createdAt;
    @UpdateTimestamp @Column(nullable=false) private Instant updatedAt;
    public Long getId(){return id;} public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public PendingRegistrationType getType(){return type;} public void setType(PendingRegistrationType v){type=v;} public Language getPreferredLanguage(){return preferredLanguage;} public void setPreferredLanguage(Language v){preferredLanguage=v;}
    public String getProfilePicture(){return profilePicture;} public void setProfilePicture(String v){profilePicture=v;}
    public Long getInvitationId(){return invitationId;} public void setInvitationId(Long v){invitationId=v;} public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;}
    public boolean isConsumed(){return consumed;} public void setConsumed(boolean v){consumed=v;}
}
