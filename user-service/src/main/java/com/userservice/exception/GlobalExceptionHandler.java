package com.userservice.exception;

import com.userservice.dto.UserDtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(UserExceptions.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            UserExceptions.UserNotFoundException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "User not found: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            UserExceptions.UserAlreadyExistsException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "User conflict: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.InvalidPhoneNumberException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPhone(
            UserExceptions.InvalidPhoneNumberException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Invalid phone: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.PhoneNotRegisteredException.class)
    public ResponseEntity<ErrorResponse> handlePhoneNotRegistered(
            UserExceptions.PhoneNotRegisteredException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Phone not registered: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.FriendRequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleFriendRequestExists(
            UserExceptions.FriendRequestAlreadyExistsException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Duplicate friend request: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.AlreadyFriendsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyFriends(
            UserExceptions.AlreadyFriendsException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Already friends: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.FriendRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFriendRequestNotFound(
            UserExceptions.FriendRequestNotFoundException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Friend request not found: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.SelfFriendRequestException.class)
    public ResponseEntity<ErrorResponse> handleSelfRequest(
            UserExceptions.SelfFriendRequestException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Self friend request: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(UserExceptions.UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UserExceptions.UnauthorizedActionException ex,
            HttpServletRequest req) {

        log.warning(String.format(
                "Unauthorized action: %s",
                ex.getMessage()
        ));

        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warning(String.format(
                "Validation error at %s: %s",
                req.getRequestURI(),
                msg
        ));

        return build(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the 5 MB limit.",
                req
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest req) {

        log.severe(String.format(
                "Unhandled error at %s: %s",
                req.getRequestURI(),
                ex.getMessage()
        ));

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                req
        );
    }

    // ── helper ────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest req) {

        ErrorResponse response =
                new ErrorResponse();

        response.setStatus(
                status.value()
        );

        response.setError(
                status.getReasonPhrase()
        );

        response.setMessage(
                message
        );

        response.setPath(
                req.getRequestURI()
        );

        response.setTimestamp(
                LocalDateTime.now().toString()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}