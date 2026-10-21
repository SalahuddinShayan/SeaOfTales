package com.seaoftales.identity;

import com.seaoftales.content.enums.Language;
import com.seaoftales.identity.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Test
    void authorInvitationEmailUsesEnglishSubjectAndBody() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "noreply@example.com", "contact@example.com");
        Instant expiration = Instant.parse("2026-10-13T12:00:00Z");

        service.sendAuthorInvitation("writer@example.com", "https://stories.example/invite/author?token=hidden", expiration, Language.ENGLISH);

        var message = capture(sender);
        assertEquals("Sea of Tales Author Invitation", message.getSubject());
        assertTrue(message.getText().contains("invited to register as an Author"));
        assertTrue(message.getText().contains("Use the link below"));
        assertTrue(message.getText().contains("2026-10-13 12:00 UTC"));
        assertTrue(message.getText().contains("intended only for the invited recipient"));
    }

    @Test
    void authorInvitationEmailUsesJapaneseSubjectAndBody() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "noreply@example.com", "contact@example.com");
        Instant expiration = Instant.parse("2026-10-13T12:00:00Z");

        service.sendAuthorInvitation("writer@example.com", "https://stories.example/invite/author?token=hidden", expiration, Language.JAPANESE);

        var message = capture(sender);
        assertEquals("Sea of Tales 作家へのご招待", message.getSubject());
        assertTrue(message.getText().contains("作家として登録"));
        assertTrue(message.getText().contains("以下のリンクから登録"));
        assertTrue(message.getText().contains("2026-10-13 12:00 UTC"));
        assertTrue(message.getText().contains("招待されたご本人専用"));
    }

    @Test
    void authorInvitationEmailUsesKoreanSubjectAndBody() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "noreply@example.com", "contact@example.com");
        Instant expiration = Instant.parse("2026-10-13T12:00:00Z");

        service.sendAuthorInvitation("writer@example.com", "https://stories.example/invite/author?token=hidden", expiration, Language.KOREAN);

        var message = capture(sender);
        assertEquals("Sea of Tales 작가 초대", message.getSubject());
        assertTrue(message.getText().contains("작가로 등록"));
        assertTrue(message.getText().contains("아래 링크에서 등록"));
        assertTrue(message.getText().contains("2026-10-13 12:00 UTC"));
        assertTrue(message.getText().contains("초대받은 분만 사용할 수"));
    }

    @Test
    void otpInstructionsUseRequestedLanguage() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender, "noreply@example.com", "contact@example.com");
        service.sendOtp("writer@example.com", "123456", "AUTHOR_REGISTRATION", Language.ENGLISH);
        service.sendOtp("writer@example.com", "123456", "AUTHOR_REGISTRATION", Language.JAPANESE);
        service.sendOtp("writer@example.com", "123456", "AUTHOR_REGISTRATION", Language.KOREAN);

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(3)).send(captor.capture());
        var messages = captor.getAllValues();
        assertTrue(messages.get(0).getText().contains("expires in 10 minutes"));
        assertTrue(messages.get(1).getText().contains("有効期限は10分"));
        assertTrue(messages.get(2).getText().contains("10분 동안 유효"));
    }

    private SimpleMailMessage capture(JavaMailSender sender) {
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }
}
