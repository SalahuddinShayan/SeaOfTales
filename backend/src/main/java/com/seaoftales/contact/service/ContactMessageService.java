package com.seaoftales.contact.service;

import com.seaoftales.contact.dto.ContactMessageDtos;
import com.seaoftales.contact.entity.ContactMessage;
import com.seaoftales.contact.enums.ContactMessageStatus;
import com.seaoftales.contact.repository.ContactMessageRepository;
import com.seaoftales.identity.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ContactMessageService {
    private static final Logger log = LoggerFactory.getLogger(ContactMessageService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_PAGE_SIZE = 50;
    private final ContactMessageRepository repository;
    private final EmailService emailService;

    public ContactMessageService(ContactMessageRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional
    public ContactMessageDtos.SubmissionResponse submit(ContactMessageDtos.PublicRequest request) {
        if (request == null || hasText(request.website())) {
            if (request != null && hasText(request.website())) return new ContactMessageDtos.SubmissionResponse("Thank you for contacting Sea of Tales.", null);
            throw new IllegalArgumentException("Please complete all required fields.");
        }
        String name = clean(request.name());
        String email = clean(request.email()).toLowerCase(Locale.ROOT);
        String subject = clean(request.subject());
        String message = request.message() == null ? "" : request.message().trim();
        if (name.isBlank() || email.isBlank() || subject.isBlank() || message.isBlank()) throw new IllegalArgumentException("Please complete all required fields.");
        if (name.length() > 100 || email.length() > 254 || subject.length() > 200 || message.length() > 10000) throw new IllegalArgumentException("One or more fields are too long.");
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Please enter a valid email address.");

        ContactMessage saved = new ContactMessage();
        saved.setName(name); saved.setEmail(email); saved.setSubject(subject); saved.setMessage(message); saved.setStatus(ContactMessageStatus.NEW);
        saved = repository.saveAndFlush(saved);
        try {
            emailService.sendContactNotification(saved);
        } catch (RuntimeException exception) {
            log.warn("Contact notification failed for messageId={} type={}", saved.getId(), exception.getClass().getSimpleName());
        }
        return new ContactMessageDtos.SubmissionResponse("Thank you for contacting Sea of Tales.", saved.getId());
    }

    @Transactional(readOnly = true)
    public ContactMessageDtos.PageResponse list(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("Invalid pagination.");
        Page<ContactMessage> result = repository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
        return new ContactMessageDtos.PageResponse(result.getContent().stream().map(this::toListItem).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ContactMessageDtos.Detail get(Long id) {
        return toDetail(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact message not found.")));
    }

    @Transactional
    public ContactMessageDtos.Detail updateStatus(Long id, ContactMessageStatus status) {
        if (status == null) throw new IllegalArgumentException("Status is required.");
        ContactMessage message = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact message not found."));
        message.setStatus(status);
        return toDetail(repository.save(message));
    }

    private ContactMessageDtos.ListItem toListItem(ContactMessage m) { return new ContactMessageDtos.ListItem(m.getId(), m.getName(), m.getEmail(), m.getSubject(), m.getStatus(), m.getCreatedAt()); }
    private ContactMessageDtos.Detail toDetail(ContactMessage m) { return new ContactMessageDtos.Detail(m.getId(), m.getName(), m.getEmail(), m.getSubject(), m.getMessage(), m.getStatus(), m.getCreatedAt(), m.getUpdatedAt()); }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
