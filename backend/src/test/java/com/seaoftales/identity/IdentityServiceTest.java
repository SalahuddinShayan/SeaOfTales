package com.seaoftales.identity;

import com.seaoftales.content.enums.Language;
import com.seaoftales.identity.entity.Invitation;
import com.seaoftales.identity.entity.PendingRegistration;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.enums.InvitationType;
import com.seaoftales.identity.enums.OtpPurpose;
import com.seaoftales.identity.enums.PendingRegistrationType;
import com.seaoftales.identity.enums.UserRole;
import com.seaoftales.identity.repository.InvitationRepository;
import com.seaoftales.identity.repository.PendingRegistrationRepository;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.identity.service.IdentityService;
import com.seaoftales.identity.service.OtpService;
import com.seaoftales.identity.service.EmailService;
import com.seaoftales.storage.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
    @Mock UserRepository users;
    @Mock InvitationRepository invitations;
    @Mock PendingRegistrationRepository pending;
    @Mock PasswordEncoder encoder;
    @Mock OtpService otp;
    @Mock EmailService emailService;
    @Mock ImageStorageService imageStorage;
    @InjectMocks IdentityService service;

    private Invitation invitation;

    @BeforeEach
    void setUp() {
        invitation = new Invitation();
        invitation.setTokenHash(IdentityService.hash("secure-token"));
        invitation.setType(InvitationType.AUTHOR);
        invitation.setRecipientEmail("writer@example.com");
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        invitation.setCreatedAt(Instant.now());
    }

    @Test
    void validAuthorInvitationStartsHashedPendingRegistration() {
        when(invitations.findByTokenHash(IdentityService.hash("secure-token"))).thenReturn(Optional.of(invitation));
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(any())).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("bcrypt-hash");

        service.registerInvitation("secure-token", InvitationType.AUTHOR, "writer_1", "writer@example.com", "password123", Language.JAPANESE);

        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pending).save(captor.capture());
        PendingRegistration saved = captor.getValue();
        assertEquals("bcrypt-hash", saved.getPasswordHash());
        assertEquals(PendingRegistrationType.AUTHOR, saved.getType());
        assertEquals(Language.JAPANESE, saved.getPreferredLanguage());
        assertNotEquals("password123", saved.getPasswordHash());
        verify(otp).issue("writer@example.com", OtpPurpose.AUTHOR_REGISTRATION, Language.JAPANESE);
        verify(users, never()).save(any());
    }

    @Test
    void authorInvitationRequiresValidEmailAndPersistsNormalizedRecipientAndSends() {
        when(invitations.save(any(Invitation.class))).thenAnswer(call -> call.getArgument(0));
        assertThrows(IllegalArgumentException.class, () -> service.createAuthorInvitation("bad-address", Language.JAPANESE, "http://localhost:3000"));
        var created = service.createAuthorInvitation("  Writer@Example.COM ", Language.JAPANESE, "http://localhost:3000");

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitations).save(captor.capture());
        Invitation saved = captor.getValue();
        assertEquals("writer@example.com", saved.getRecipientEmail());
        assertEquals(Language.JAPANESE, saved.getPreferredLanguage());
        assertEquals(InvitationType.AUTHOR, saved.getType());
        assertTrue(created.emailSent());
        verify(emailService).sendAuthorInvitation(eq("writer@example.com"), contains("/invite/author?token="), eq(created.expiresAt()), eq(Language.JAPANESE));
    }

    @Test
    void failedMailIsReportedAndExistingValidInvitationCanBeResent() {
        doThrow(new IllegalStateException("SMTP unavailable")).when(emailService).sendAuthorInvitation(any(), any(), any(), any());
        when(invitations.save(any(Invitation.class))).thenAnswer(call -> call.getArgument(0));
        var created = service.createAuthorInvitation("writer@example.com", Language.KOREAN, "http://localhost:3000");
        assertFalse(created.emailSent());

        when(invitations.findByTokenHash(IdentityService.hash("secure-token"))).thenReturn(Optional.of(invitation));
        invitation.setPreferredLanguage(Language.KOREAN);
        doNothing().when(emailService).sendAuthorInvitation(any(), any(), any(), any());
        var resent = service.resendAuthorInvitation("secure-token", "http://localhost:3000");
        assertTrue(resent.emailSent());
        verify(invitations, times(1)).save(any(Invitation.class));
        verify(emailService).sendAuthorInvitation(eq("writer@example.com"), contains("/invite/author?token=secure-token"), eq(invitation.getExpiresAt()), eq(Language.KOREAN));
    }

    @Test
    void invitationWithoutStoredLanguageResendsUsingJapaneseLegacyFallback() {
        when(invitations.findByTokenHash(IdentityService.hash("secure-token"))).thenReturn(Optional.of(invitation));
        service.resendAuthorInvitation("secure-token", "http://localhost:3000");
        verify(emailService).sendAuthorInvitation(eq("writer@example.com"), any(), eq(invitation.getExpiresAt()), eq(Language.JAPANESE));
    }

    @Test
    void englishInvitationLanguageIsSavedThroughOtpVerification() {
        when(invitations.save(any(Invitation.class))).thenAnswer(call -> call.getArgument(0));
        var created = service.createAuthorInvitation("writer@example.com", Language.ENGLISH, "http://localhost:3000");
        assertEquals(Language.ENGLISH, created.preferredLanguage());

        when(invitations.findByTokenHash(IdentityService.hash("secure-token"))).thenReturn(Optional.of(invitation));
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(any())).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("bcrypt-hash");
        service.registerInvitation("secure-token", InvitationType.AUTHOR, "writer_1", "writer@example.com", "password123", Language.ENGLISH);
        ArgumentCaptor<PendingRegistration> pendingCaptor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pending, atLeastOnce()).save(pendingCaptor.capture());
        assertEquals(Language.ENGLISH, pendingCaptor.getValue().getPreferredLanguage());
        verify(otp).issue("writer@example.com", OtpPurpose.AUTHOR_REGISTRATION, Language.ENGLISH);

        PendingRegistration registration = pendingCaptor.getValue();
        registration.setInvitationId(42L);
        when(pending.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc("writer@example.com")).thenReturn(Optional.of(registration));
        when(invitations.findByIdForUpdate(42L)).thenReturn(Optional.of(invitation));
        service.verifyRegistration("writer@example.com", "123456");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals(Language.ENGLISH, userCaptor.getValue().getPreferredLanguage());
    }

    @Test
    void authorRegistrationRequiresInvitedEmailAndPreservesInvitationOnMismatch() {
        when(invitations.findByTokenHash(IdentityService.hash("secure-token"))).thenReturn(Optional.of(invitation));
        assertThrows(IllegalArgumentException.class, () -> service.registerInvitation("secure-token", InvitationType.AUTHOR, "writer_1", "other@example.com", "password123", Language.JAPANESE));
        verifyNoInteractions(pending, otp);

        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail("writer@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("bcrypt-hash");
        service.registerInvitation("secure-token", InvitationType.AUTHOR, "writer_1", " WRITER@EXAMPLE.COM ", "password123", Language.JAPANESE);
        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pending).save(captor.capture());
        assertEquals("writer@example.com", captor.getValue().getEmail());
        assertFalse(invitation.isUsed());
    }

    @Test
    void rejectsWrongTypeUsedExpiredAndUnknownInvitations() {
        when(invitations.findByTokenHash(IdentityService.hash("secure-token"))).thenReturn(Optional.of(invitation));
        assertThrows(IllegalArgumentException.class, () -> service.registerInvitation("secure-token", InvitationType.EDITOR, "writer_1", "writer@example.com", "password123", Language.JAPANESE));
        invitation.setUsed(true);
        assertThrows(IllegalArgumentException.class, () -> service.registerInvitation("secure-token", InvitationType.AUTHOR, "writer_1", "writer@example.com", "password123", Language.JAPANESE));
        invitation.setUsed(false);
        invitation.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> service.registerInvitation("secure-token", InvitationType.AUTHOR, "writer_1", "writer@example.com", "password123", Language.JAPANESE));
        assertThrows(IllegalArgumentException.class, () -> service.registerInvitation("missing", InvitationType.AUTHOR, "writer_1", "writer@example.com", "password123", Language.JAPANESE));
        verifyNoInteractions(pending, otp);
    }

    @Test
    void verifiesPurposeBeforePersistingAuthorAndConsumingInvitation() {
        PendingRegistration registration = new PendingRegistration();
        registration.setUsername("writer_1"); registration.setEmail("writer@example.com"); registration.setPasswordHash("bcrypt-hash");
        registration.setType(PendingRegistrationType.AUTHOR); registration.setPreferredLanguage(Language.KOREAN);
        registration.setInvitationId(42L); registration.setExpiresAt(Instant.now().plusSeconds(3600));
        when(pending.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(registration.getEmail())).thenReturn(Optional.of(registration));
        when(invitations.findByIdForUpdate(42L)).thenReturn(Optional.of(invitation));
        when(users.existsByUsername(registration.getUsername())).thenReturn(false);
        when(users.existsByEmail(registration.getEmail())).thenReturn(false);

        service.verifyRegistration(registration.getEmail(), "123456");

        verify(otp).verify(registration.getEmail(), OtpPurpose.AUTHOR_REGISTRATION, "123456");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals(UserRole.AUTHOR, userCaptor.getValue().getRole());
        assertEquals(Language.KOREAN, userCaptor.getValue().getPreferredLanguage());
        assertTrue(invitation.isUsed());
        assertTrue(registration.isConsumed());
    }

    @Test
    void invalidOtpDoesNotCreateUserOrConsumeInvitation() {
        PendingRegistration registration = new PendingRegistration();
        registration.setUsername("writer_1"); registration.setEmail("writer@example.com"); registration.setPasswordHash("bcrypt-hash");
        registration.setType(PendingRegistrationType.AUTHOR); registration.setInvitationId(42L); registration.setExpiresAt(Instant.now().plusSeconds(3600));
        when(pending.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(registration.getEmail())).thenReturn(Optional.of(registration));
        when(invitations.findByIdForUpdate(42L)).thenReturn(Optional.of(invitation));
        doThrow(new IllegalArgumentException("Invalid or expired code.")).when(otp).verify(registration.getEmail(), OtpPurpose.AUTHOR_REGISTRATION, "000000");

        assertThrows(IllegalArgumentException.class, () -> service.verifyRegistration(registration.getEmail(), "000000"));
        verify(users, never()).save(any());
        assertFalse(invitation.isUsed());
        assertFalse(registration.isConsumed());
    }

    @Test
    void verificationUsesPictureFromReplacementState() {
        PendingRegistration registration = new PendingRegistration();
        registration.setUsername("reader_1"); registration.setEmail("reader@example.com");
        registration.setPasswordHash("bcrypt-hash"); registration.setType(PendingRegistrationType.READER);
        registration.setProfilePicture("replacement.jpg"); registration.setExpiresAt(Instant.now().plusSeconds(3600));
        when(pending.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(registration.getEmail())).thenReturn(Optional.of(registration));
        when(users.existsByUsername(registration.getUsername())).thenReturn(false);
        when(users.existsByEmail(registration.getEmail())).thenReturn(false);

        service.verifyRegistration(registration.getEmail(), "123456");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals("replacement.jpg", userCaptor.getValue().getProfilePicture());
    }

    @Test
    void consumedRegistrationCannotBeReplacedAndItsPictureRemainsReferenced() {
        PendingRegistration registration = new PendingRegistration();
        registration.setUsername("reader_1"); registration.setEmail("reader@example.com");
        registration.setPasswordHash("bcrypt-hash"); registration.setType(PendingRegistrationType.READER);
        registration.setProfilePicture("completed.jpg"); registration.setExpiresAt(Instant.now().plusSeconds(3600));
        when(pending.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(registration.getEmail()))
                .thenAnswer(invocation -> registration.isConsumed() ? Optional.empty() : Optional.of(registration));
        when(users.existsByUsername(registration.getUsername())).thenReturn(false, true);
        when(users.existsByEmail(registration.getEmail())).thenReturn(false, true);

        service.verifyRegistration(registration.getEmail(), "123456");
        assertThrows(IllegalStateException.class, () -> service.register(registration.getUsername(), registration.getEmail(), "password123", "replacement.jpg"));

        verify(imageStorage, never()).delete("completed.jpg");
        assertEquals("completed.jpg", registration.getProfilePicture());
    }

    @Test
    void rechecksAccountExistenceAfterPendingRegistrationLookup() {
        PendingRegistration registration = new PendingRegistration();
        registration.setEmail("reader@example.com");
        registration.setProfilePicture("previous.jpg");
        when(pending.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc("reader@example.com"))
                .thenReturn(Optional.of(registration));
        when(users.existsByUsername(any())).thenReturn(false, true);
        when(users.existsByEmail(any())).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> service.register("reader_1", "reader@example.com", "password123", "replacement.jpg"));

        verify(pending, never()).save(any());
        verify(otp, never()).issue(any(), eq(OtpPurpose.REGISTRATION));
        verify(imageStorage, never()).delete("previous.jpg");
        assertEquals("previous.jpg", registration.getProfilePicture());
    }
}
