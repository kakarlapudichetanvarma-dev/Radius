package com.chatservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiDtos {

    // ── Generic AI chat/coding assistant request (sent over STOMP) ─────────
    public static class AiChatRequest {
        @NotBlank(message = "message is required")
        private String message;
        private String conversationId;   // null = use/create the user's active conversation
        private String contextChatId;    // the chat the user currently has open, if any (used by SUMMARIZE command)

        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String v) { this.conversationId = v; }
        public String getContextChatId() { return contextChatId; }
        public void setContextChatId(String v) { this.contextChatId = v; }
    }

    // ── Generic AI chat/coding assistant response (pushed to /user/queue/ai) ─
    public static class AiChatResponse {
        private String conversationId;
        private String role; // "MODEL"
        private String content;
        private String sentAt;
        private String type; // "CHAT_REPLY" | "CODE_REPLY" | "ERROR" | "GROUP_SEND_CONFIRMATION" | "SUMMARY"

        public String getConversationId() { return conversationId; }
        public void setConversationId(String v) { this.conversationId = v; }
        public String getRole() { return role; }
        public void setRole(String v) { this.role = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String v) { this.sentAt = v; }
        public String getType() { return type; }
        public void setType(String v) { this.type = v; }
    }

    // ── Conversation history response (REST) ────────────────────────────────
    public static class AiConversationHistoryResponse {
        private String conversationId;
        private String conversationType;
        private List<AiMessageItem> messages;

        public String getConversationId() { return conversationId; }
        public void setConversationId(String v) { this.conversationId = v; }
        public String getConversationType() { return conversationType; }
        public void setConversationType(String v) { this.conversationType = v; }
        public List<AiMessageItem> getMessages() { return messages; }
        public void setMessages(List<AiMessageItem> v) { this.messages = v; }
    }

    public static class AiMessageItem {
        private String role;
        private String content;
        private String sentAt;

        public String getRole() { return role; }
        public void setRole(String v) { this.role = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String v) { this.sentAt = v; }
    }

    // ── Coding assistant specific request ───────────────────────────────────
    public static class CodeAssistRequest {
        @NotBlank(message = "prompt is required")
        private String prompt;          // e.g. "explain this error" / "review this code"
        private String code;             // optional code snippet
        private String errorMessage;     // optional stack trace / exception text
        private String language;         // "java" | "typescript" | "spring-boot" etc.
        private String conversationId;

        public String getPrompt() { return prompt; }
        public void setPrompt(String v) { this.prompt = v; }
        public String getCode() { return code; }
        public void setCode(String v) { this.code = v; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String v) { this.errorMessage = v; }
        public String getLanguage() { return language; }
        public void setLanguage(String v) { this.language = v; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String v) { this.conversationId = v; }
    }

    // ── Smart Reply ──────────────────────────────────────────────────────────
    public static class SmartReplyRequest {
        @NotBlank(message = "chatId is required")
        private String chatId;

        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
    }

    public static class SmartReplyResponse {
        private String chatId;
        private List<String> suggestions;

        public SmartReplyResponse() {}
        public SmartReplyResponse(String chatId, List<String> suggestions) {
            this.chatId = chatId;
            this.suggestions = suggestions;
        }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> v) { this.suggestions = v; }
    }

    // ── Translation ──────────────────────────────────────────────────────────
    public static class TranslateRequest {
        @NotBlank(message = "text is required")
        private String text;
        @NotBlank(message = "targetLanguage is required")
        private String targetLanguage; // e.g. "German", "Spanish", "hi", "de" - free text, Gemini handles it

        public String getText() { return text; }
        public void setText(String v) { this.text = v; }
        public String getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(String v) { this.targetLanguage = v; }
    }

    public static class TranslateResponse {
        private String originalText;
        private String translatedText;
        private String targetLanguage;

        public TranslateResponse() {}
        public TranslateResponse(String originalText, String translatedText, String targetLanguage) {
            this.originalText = originalText;
            this.translatedText = translatedText;
            this.targetLanguage = targetLanguage;
        }
        public String getOriginalText() { return originalText; }
        public void setOriginalText(String v) { this.originalText = v; }
        public String getTranslatedText() { return translatedText; }
        public void setTranslatedText(String v) { this.translatedText = v; }
        public String getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(String v) { this.targetLanguage = v; }
    }

    // ── Grammar Correction ──────────────────────────────────────────────────
    public static class GrammarCorrectionRequest {
        @NotBlank(message = "text is required")
        private String text;
        private String tone = "casual"; // "casual" | "professional"

        public String getText() { return text; }
        public void setText(String v) { this.text = v; }
        public String getTone() { return tone; }
        public void setTone(String v) { this.tone = v; }
    }

    public static class GrammarCorrectionResponse {
        private String originalText;
        private String correctedText;

        public GrammarCorrectionResponse() {}
        public GrammarCorrectionResponse(String originalText, String correctedText) {
            this.originalText = originalText;
            this.correctedText = correctedText;
        }
        public String getOriginalText() { return originalText; }
        public void setOriginalText(String v) { this.originalText = v; }
        public String getCorrectedText() { return correctedText; }
        public void setCorrectedText(String v) { this.correctedText = v; }
    }

    // ── Summarization ────────────────────────────────────────────────────────
    public static class SummarizeRequest {
        @NotBlank(message = "chatId is required")
        private String chatId;
        private Integer messageLimit; // optional cap on how many recent messages to summarize

        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public Integer getMessageLimit() { return messageLimit; }
        public void setMessageLimit(Integer v) { this.messageLimit = v; }
    }

    public static class SummarizeResponse {
        private String chatId;
        private String summary;

        public SummarizeResponse() {}
        public SummarizeResponse(String chatId, String summary) {
            this.chatId = chatId;
            this.summary = summary;
        }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getSummary() { return summary; }
        public void setSummary(String v) { this.summary = v; }
    }

    // ── Group Automation command result ─────────────────────────────────────
    public static class GroupCommandResult {
        private boolean success;
        private String resolvedGroupName;
        private String chatId;
        private String sentMessageId;
        private String message; // human-readable confirmation or error

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getResolvedGroupName() { return resolvedGroupName; }
        public void setResolvedGroupName(String v) { this.resolvedGroupName = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getSentMessageId() { return sentMessageId; }
        public void setSentMessageId(String v) { this.sentMessageId = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }
}