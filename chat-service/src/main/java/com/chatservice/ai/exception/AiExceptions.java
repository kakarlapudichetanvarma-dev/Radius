package com.chatservice.ai.exception;

public class AiExceptions {

    public static class AiProviderException extends RuntimeException {
        public AiProviderException(String message) { super(message); }
        public AiProviderException(String message, Throwable cause) { super(message, cause); }
    }

    public static class AiQuotaExceededException extends RuntimeException {
        public AiQuotaExceededException(String message) { super(message); }
    }

    public static class GroupNotResolvedException extends RuntimeException {
        public GroupNotResolvedException(String message) { super(message); }
    }

    public static class AmbiguousGroupNameException extends RuntimeException {
        public AmbiguousGroupNameException(String message) { super(message); }
    }

    public static class UnsupportedAiCommandException extends RuntimeException {
        public UnsupportedAiCommandException(String message) { super(message); }
    }

    public static class ConversationNotFoundException extends RuntimeException {
        public ConversationNotFoundException(String message) { super(message); }
    }
}