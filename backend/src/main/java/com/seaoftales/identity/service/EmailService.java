package com.seaoftales.identity.service;

import com.seaoftales.contact.entity.ContactMessage;
import com.seaoftales.content.enums.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    private final JavaMailSender sender;
    private final String from;
    private final String contactRecipient;

    public EmailService(JavaMailSender sender,
                        @Value("${spring.mail.from:noreply@seaoftales.local}") String from,
                        @Value("${seaoftales.contact.recipient:contact@easternwordsmith.com}") String contactRecipient) {
        this.sender = sender;
        this.from = from;
        this.contactRecipient = contactRecipient;
    }

    public void sendOtp(String email, String code, String purpose) {
        sendOtp(email, code, purpose, Language.ENGLISH);
    }

    public void sendOtp(String email, String code, String purpose, Language language) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        if (language == Language.JAPANESE) {
            message.setSubject("Sea of Tales メール認証コード");
            message.setText("Sea of Tales の" + purposeLabel(purpose, language) + "コードは " + code
                    + " です。有効期限は10分です。このコードを他の人に教えないでください。");
        } else if (language == Language.KOREAN) {
            message.setSubject("Sea of Tales 이메일 인증 코드");
            message.setText("Sea of Tales " + purposeLabel(purpose, language) + " 코드는 " + code
                    + "입니다. 코드는 10분 동안 유효하며 다른 사람과 공유하지 마세요.");
        } else {
            message.setSubject("Sea of Tales verification code");
            message.setText("Your Sea of Tales " + purpose.toLowerCase().replace('_', ' ')
                    + " code is " + code + ". It expires in 10 minutes. Do not share it.");
        }
        sender.send(message);
    }

    private String purposeLabel(String purpose, Language language) {
        if (purpose.equals("AUTHOR_REGISTRATION")) return language == Language.JAPANESE ? "作家登録" : "작가 등록";
        if (purpose.equals("REGISTRATION")) return language == Language.JAPANESE ? "アカウント登録" : "계정 등록";
        if (purpose.equals("PASSWORD_RESET")) return language == Language.JAPANESE ? "パスワード再設定" : "비밀번호 재설정";
        return language == Language.JAPANESE ? "メール認証" : "이메일 인증";
    }

    public void sendAuthorInvitation(String email, String invitationUrl, Instant expiresAt, Language language) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        String expiration = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(ZoneOffset.UTC).format(expiresAt);
        if (language == Language.KOREAN) {
            message.setSubject("Sea of Tales 작가 초대");
            message.setText("Sea of Tales 작가로 등록해 달라는 초대를 받으셨습니다.\n\n"
                    + "아래 링크에서 등록을 진행해 주세요:\n" + invitationUrl + "\n\n"
                    + "초대 링크는 " + expiration + "에 만료됩니다. 이 링크는 초대받은 분만 사용할 수 있으며 다른 사람과 공유하지 마세요.");
        } else if (language == Language.JAPANESE) {
            message.setSubject("Sea of Tales 作家へのご招待");
            message.setText("Sea of Tales の作家として登録するための招待をお送りしました。\n\n"
                    + "以下のリンクから登録してください。\n" + invitationUrl + "\n\n"
                    + "招待リンクの有効期限は " + expiration + " です。このリンクは招待されたご本人専用です。他の方と共有しないでください。");
        } else {
            message.setSubject("Sea of Tales Author Invitation");
            message.setText("You have been invited to register as an Author on Sea of Tales.\n\n"
                    + "Use the link below to complete your registration:\n" + invitationUrl + "\n\n"
                    + "This invitation expires on " + expiration + ". This link is intended only for the invited recipient and must not be shared.");
        }
        sender.send(message);
    }

    public void sendContactNotification(ContactMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(contactRecipient);
        mail.setReplyTo(message.getEmail());
        mail.setSubject("Sea of Tales contact: " + message.getSubject());
        mail.setText("Contact message #" + message.getId() + "\n\nName: " + message.getName()
                + "\nEmail: " + message.getEmail() + "\nSubject: " + message.getSubject()
                + "\n\n" + message.getMessage());
        sender.send(mail);
    }
}
