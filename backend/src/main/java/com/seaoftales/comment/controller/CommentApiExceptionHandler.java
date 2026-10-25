package com.seaoftales.comment.controller;

import com.seaoftales.comment.dto.CommentDtos;
import com.seaoftales.comment.service.CommentApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice(basePackageClasses = CommentController.class)
public class CommentApiExceptionHandler {
    @ExceptionHandler(CommentApiException.class)
    public ResponseEntity<CommentDtos.Error> commentError(CommentApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new CommentDtos.Error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<CommentDtos.Error> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new CommentDtos.Error("invalid_request", "The request is malformed or contains invalid values."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommentDtos.Error> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CommentDtos.Error("internal_error", "The comment request could not be completed."));
    }
}
