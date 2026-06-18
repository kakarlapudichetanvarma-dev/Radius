package com.chatservice.exception;

import com.chatservice.dto.ChatDtos.ErrorResponse;
import com.chatservice.exception.ChatExceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.chatservice.ai.exception.AiExceptions.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotFound(
            MessageNotFoundException ex, HttpServletRequest req) {
        log.warn("MessageNotFound: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatNotFound(
            ChatNotFoundException ex, HttpServletRequest req) {
        log.warn("ChatNotFound: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGroupNotFound(
            GroupNotFoundException ex, HttpServletRequest req) {
        log.warn("GroupNotFound: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NotChatMemberException.class)
    public ResponseEntity<ErrorResponse> handleNotMember(
            NotChatMemberException ex, HttpServletRequest req) {
        log.warn("NotChatMember: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(NotGroupAdminException.class)
    public ResponseEntity<ErrorResponse> handleNotAdmin(
            NotGroupAdminException ex, HttpServletRequest req) {
        log.warn("NotGroupAdmin: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(AlreadyMemberException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyMember(
            AlreadyMemberException ex, HttpServletRequest req) {
        log.warn("AlreadyMember: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedMessageActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedMessageActionException ex, HttpServletRequest req) {
        log.warn("UnauthorizedMessageAction: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failure on {}: {}", req.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("File too large: {}", ex.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit (20MB).", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", req);
    }

     @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderException(
            AiProviderException ex, HttpServletRequest req) {
        log.error("AiProviderException: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(
            ConversationNotFoundException ex, HttpServletRequest req) {
        log.warn("ConversationNotFound: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(GroupNotResolvedException.class)
    public ResponseEntity<ErrorResponse> handleGroupNotResolved(
            GroupNotResolvedException ex, HttpServletRequest req) {
        log.warn("GroupNotResolved: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(AmbiguousGroupNameException.class)
    public ResponseEntity<ErrorResponse> handleAmbiguousGroupName(
            AmbiguousGroupNameException ex, HttpServletRequest req) {
        log.warn("AmbiguousGroupName: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse();
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setMessage(message);
        body.setPath(req.getRequestURI());
        body.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
