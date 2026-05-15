package com.authservice.exception;

import com.authservice.dto.AuthDtos.ErrorResponse;

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

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );


    @ExceptionHandler(
            AuthExceptions.UserAlreadyExistsException.class
    )
    public ResponseEntity<ErrorResponse>
    handleUserAlreadyExists(

            AuthExceptions.UserAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn(
                "User already exists: {}",
                ex.getMessage()
        );

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(
            AuthExceptions.UserNotFoundException.class
    )
    public ResponseEntity<ErrorResponse>
    handleUserNotFound(

            AuthExceptions.UserNotFoundException ex,
            HttpServletRequest request) {

        log.warn(
                "User not found: {}",
                ex.getMessage()
        );

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(
            AuthExceptions.InvalidCredentialsException.class
    )
    public ResponseEntity<ErrorResponse>
    handleInvalidCredentials(

            AuthExceptions.InvalidCredentialsException ex,
            HttpServletRequest request) {

        log.warn(
                "Invalid credentials attempt for path: {}",
                request.getRequestURI()
        );

        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(
            AuthExceptions.InvalidOtpException.class
    )
    public ResponseEntity<ErrorResponse>
    handleInvalidOtp(

            AuthExceptions.InvalidOtpException ex,
            HttpServletRequest request) {

        log.warn(
                "Invalid OTP attempt: {}",
                ex.getMessage()
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(
            AuthExceptions.OtpExpiredException.class
    )
    public ResponseEntity<ErrorResponse>
    handleOtpExpired(

            AuthExceptions.OtpExpiredException ex,
            HttpServletRequest request) {

        log.warn(
                "OTP expired: {}",
                ex.getMessage()
        );

        return buildError(
                HttpStatus.GONE,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(
            AuthExceptions.AccountInactiveException.class
    )
    public ResponseEntity<ErrorResponse>
    handleAccountInactive(

            AuthExceptions.AccountInactiveException ex,
            HttpServletRequest request) {

        log.warn(
                "Inactive account login attempt: {}",
                ex.getMessage()
        );

        return buildError(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request
        );
    }


    @ExceptionHandler(
            AuthExceptions.EmailSendException.class
    )
    public ResponseEntity<ErrorResponse>
    handleEmailSend(

            AuthExceptions.EmailSendException ex,
            HttpServletRequest request) {

        log.error(
                "Email send failure: {}",
                ex.getMessage(),
                ex.getCause()
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to send email. Try again later.",
                request
        );
    }


    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ErrorResponse>
    handleValidation(

            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(
                                FieldError::getDefaultMessage
                        )
                        .collect(
                                Collectors.joining("; ")
                        );

        log.warn(
                "Validation failure on {}: {}",
                request.getRequestURI(),
                message
        );

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }


    @ExceptionHandler(
            MaxUploadSizeExceededException.class
    )
    public ResponseEntity<ErrorResponse>
    handleMaxUploadSize(

            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.warn(
                "File upload too large: {}",
                ex.getMessage()
        );

        return buildError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit (5MB).",
                request
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    handleGeneric(

            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Unhandled exception at {}: {}",
                request.getRequestURI(),
                ex.getMessage(),
                ex
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request
        );
    }


    private ResponseEntity<ErrorResponse>
    buildError(

            HttpStatus status,
            String message,
            HttpServletRequest request) {

        ErrorResponse body =
                new ErrorResponse();

        body.setStatus(
                status.value()
        );

        body.setError(
                status.getReasonPhrase()
        );

        body.setMessage(
                message
        );

        body.setPath(
                request.getRequestURI()
        );

        body.setTimestamp(
                LocalDateTime.now()
                        .toString()
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}