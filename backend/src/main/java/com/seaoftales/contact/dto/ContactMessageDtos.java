package com.seaoftales.contact.dto;

import com.seaoftales.contact.enums.ContactMessageStatus;

import java.time.Instant;
import java.util.List;

public final class ContactMessageDtos {
    private ContactMessageDtos() { }

    public record PublicRequest(String name, String email, String subject, String message, String website) { }
    public record SubmissionResponse(String message, Long id) { }
    public record ListItem(Long id, String name, String email, String subject, ContactMessageStatus status, Instant createdAt) { }
    public record Detail(Long id, String name, String email, String subject, String message, ContactMessageStatus status, Instant createdAt, Instant updatedAt) { }
    public record PageResponse(List<ListItem> content, int page, int size, long totalElements, int totalPages) { }
    public record StatusRequest(ContactMessageStatus status) { }
}
