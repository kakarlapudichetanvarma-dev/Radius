package com.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class UserExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidPhoneNumberException extends RuntimeException {
        public InvalidPhoneNumberException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class PhoneNotRegisteredException extends RuntimeException {
        public PhoneNotRegisteredException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class FriendRequestAlreadyExistsException extends RuntimeException {
        public FriendRequestAlreadyExistsException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class AlreadyFriendsException extends RuntimeException {
        public AlreadyFriendsException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class FriendRequestNotFoundException extends RuntimeException {
        public FriendRequestNotFoundException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class SelfFriendRequestException extends RuntimeException {
        public SelfFriendRequestException(String msg) { super(msg); }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class UnauthorizedActionException extends RuntimeException {
        public UnauthorizedActionException(String msg) { super(msg); }
    }
}
