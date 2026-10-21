package com.seaoftales.contact;

import com.seaoftales.contact.dto.ContactMessageDtos;
import com.seaoftales.contact.entity.ContactMessage;
import com.seaoftales.contact.enums.ContactMessageStatus;
import com.seaoftales.contact.repository.ContactMessageRepository;
import com.seaoftales.contact.service.ContactMessageService;
import com.seaoftales.identity.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {
    @Mock ContactMessageRepository repository;
    @Mock EmailService emailService;
    @InjectMocks ContactMessageService service;

    @Test
    void validSubmissionPersistsNewMessageAndAttemptsNotification() {
        ContactMessage saved = new ContactMessage();
        saved.setName("Reader"); saved.setEmail("reader@example.com"); saved.setSubject("Hello"); saved.setMessage("A question"); saved.setStatus(ContactMessageStatus.NEW);
        when(repository.saveAndFlush(any(ContactMessage.class))).thenReturn(saved);

        ContactMessageDtos.SubmissionResponse response = service.submit(new ContactMessageDtos.PublicRequest(" Reader ", "reader@example.com", "Hello", "A question", ""));

        assertEquals("Thank you for contacting Sea of Tales.", response.message());
        verify(repository).saveAndFlush(any(ContactMessage.class));
        verify(emailService).sendContactNotification(saved);
    }

    @Test
    void notificationFailureDoesNotFailSavedSubmission() {
        ContactMessage saved = new ContactMessage();
        saved.setStatus(ContactMessageStatus.NEW);
        when(repository.saveAndFlush(any(ContactMessage.class))).thenReturn(saved);
        doThrow(new RuntimeException("SMTP unavailable")).when(emailService).sendContactNotification(saved);

        assertDoesNotThrow(() -> service.submit(new ContactMessageDtos.PublicRequest("Reader", "reader@example.com", "Hello", "A question", "")));
        verify(repository).saveAndFlush(any(ContactMessage.class));
    }

    @Test
    void invalidSubmissionIsRejectedBeforePersistence() {
        assertThrows(IllegalArgumentException.class, () -> service.submit(new ContactMessageDtos.PublicRequest("", "not-an-email", "", "", "")));
        verifyNoInteractions(repository, emailService);
    }

    @Test
    void honeypotSubmissionDoesNotPersist() {
        ContactMessageDtos.SubmissionResponse response = service.submit(new ContactMessageDtos.PublicRequest("Reader", "reader@example.com", "Hello", "A question", "https://spam.example"));
        assertNull(response.id());
        verifyNoInteractions(repository, emailService);
    }
}
