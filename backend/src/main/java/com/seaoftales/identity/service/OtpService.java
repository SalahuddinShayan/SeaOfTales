package com.seaoftales.identity.service;

import com.seaoftales.content.enums.Language;
import com.seaoftales.identity.entity.EmailOtp;
import com.seaoftales.identity.enums.OtpPurpose;
import com.seaoftales.identity.repository.EmailOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final EmailOtpRepository repo;
    private final EmailService email;
    private final SecureRandom random = new SecureRandom();

    public OtpService(EmailOtpRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }

    public void issue(String address, OtpPurpose purpose) {
        issue(address, purpose, Language.ENGLISH);
    }

    public void issue(String address, OtpPurpose purpose, Language language) {
        var old = repo.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(address, purpose).orElse(null);
        if (old != null && old.getCreatedAt().plusSeconds(60).isAfter(Instant.now())) {
            throw new IllegalStateException("Please wait before requesting another code.");
        }
        if (old != null) {
            old.setUsed(true);
            repo.save(old);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        EmailOtp otp = new EmailOtp();
        otp.setEmail(address);
        otp.setPurpose(purpose);
        otp.setCodeHash(IdentityService.hash(code));
        otp.setCreatedAt(Instant.now());
        otp.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        repo.save(otp);
        log.info("OTP repository save returned for purpose {}", purpose);
        try {
            log.info("OTP email delivery started for purpose {}", purpose);
            email.sendOtp(address, code, purpose.name(), language);
            log.info("OTP email delivery completed for purpose {}", purpose);
        } catch (RuntimeException e) {
            log.error("OTP email delivery failed for purpose {}: {}: {}", purpose, e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    public void verify(String address, OtpPurpose purpose, String code) {
        var otp = repo.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(address, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code."));
        if (otp.getExpiresAt().isBefore(Instant.now()) || otp.getAttempts() >= 5) {
            throw new IllegalArgumentException("Invalid or expired code.");
        }
        if (code == null || !IdentityService.hash(code).equals(otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= 5) otp.setUsed(true);
            repo.save(otp);
            throw new IllegalArgumentException("Invalid or expired code.");
        }
        otp.setUsed(true);
        repo.save(otp);
    }
}
