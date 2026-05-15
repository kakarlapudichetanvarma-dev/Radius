package com.chatservice.exception;

public class ChatExceptions {

    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String message) { super(message); }
    }

    public static class ChatNotFoundException extends RuntimeException {
        public ChatNotFoundException(String message) { super(message); }
    }

    public static class GroupNotFoundException extends RuntimeException {
        public GroupNotFoundException(String message) { super(message); }
    }

    public static class NotChatMemberException extends RuntimeException {
        public NotChatMemberException(String message) { super(message); }
    }

    public static class NotGroupAdminException extends RuntimeException {
        public NotGroupAdminException(String message) { super(message); }
    }

    public static class AlreadyMemberException extends RuntimeException {
        public AlreadyMemberException(String message) { super(message); }
    }

    public static class UnauthorizedMessageActionException extends RuntimeException {
        public UnauthorizedMessageActionException(String message) { super(message); }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }
}
