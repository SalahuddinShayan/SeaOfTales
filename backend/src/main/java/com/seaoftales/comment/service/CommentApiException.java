package com.seaoftales.comment.service;

import org.springframework.http.HttpStatus;

public class CommentApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CommentApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static CommentApiException badRequest(String message) {
        return new CommentApiException(HttpStatus.BAD_REQUEST, "invalid_request", message);
    }

    public static CommentApiException notFound(String message) {
        return new CommentApiException(HttpStatus.NOT_FOUND, "not_found", message);
    }

    public static CommentApiException forbidden() {
        return new CommentApiException(HttpStatus.FORBIDDEN, "forbidden", "You are not allowed to perform this action.");
    }

    public static CommentApiException conflict(String code, String message) {
        return new CommentApiException(HttpStatus.CONFLICT, code, message);
    }

    public static CommentApiException tooManyRequests(long retryAfterSeconds) {
        return new CommentApiException(HttpStatus.TOO_MANY_REQUESTS, "posting_cooldown",
                "Please wait " + Math.max(1, retryAfterSeconds) + " seconds before posting again.");
    }
}
