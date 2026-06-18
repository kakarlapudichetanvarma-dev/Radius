package com.chatservice.ai.command;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiCommandParser {

    public enum CommandType {
        GROUP_SEND,
        SUMMARIZE,
        TRANSLATE,
        GRAMMAR_FIX,
        PLAIN_CHAT
    }

    public static class ParsedCommand {
        private final CommandType type;
        private final String groupName;
        private final String messageContent;
        private final String targetLanguage;
        private final String rawText;

        public ParsedCommand(CommandType type, String groupName, String messageContent,
                              String targetLanguage, String rawText) {
            this.type = type;
            this.groupName = groupName;
            this.messageContent = messageContent;
            this.targetLanguage = targetLanguage;
            this.rawText = rawText;
        }

        public CommandType getType() { return type; }
        public String getGroupName() { return groupName; }
        public String getMessageContent() { return messageContent; }
        public String getTargetLanguage() { return targetLanguage; }
        public String getRawText() { return rawText; }
    }

    // Matches: send "Meeting starts at 5 PM" to the Development Team group
    // Matches: send Meeting starts at 5 PM to Development Team
    private static final Pattern GROUP_SEND_PATTERN = Pattern.compile(
            "send\\s+[\"']?(.+?)[\"']?\\s+to\\s+(?:the\\s+)?(.+?)(?:\\s+group)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Matches: summarize this conversation / summarize this chat
    private static final Pattern SUMMARIZE_PATTERN = Pattern.compile(
            "summarize\\s+(?:this\\s+)?(?:conversation|chat)", Pattern.CASE_INSENSITIVE);

    // Matches: translate this message/text to German
    private static final Pattern TRANSLATE_PATTERN = Pattern.compile(
            "translate\\s+(?:this\\s+(?:message|text)\\s+)?to\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern GRAMMAR_PATTERN = Pattern.compile(
            "(improve|fix|correct)\\s+(my\\s+)?grammar", Pattern.CASE_INSENSITIVE);

    public ParsedCommand parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ParsedCommand(CommandType.PLAIN_CHAT, null, null, null, rawText);
        }

        String trimmed = rawText.trim();

        Matcher groupSendMatcher = GROUP_SEND_PATTERN.matcher(trimmed);
        if (groupSendMatcher.matches() && trimmed.toLowerCase().startsWith("send")) {
            return new ParsedCommand(CommandType.GROUP_SEND,
                    groupSendMatcher.group(2).trim(), groupSendMatcher.group(1).trim(), null, rawText);
        }

        if (SUMMARIZE_PATTERN.matcher(trimmed).find()) {
            return new ParsedCommand(CommandType.SUMMARIZE, null, null, null, rawText);
        }

        Matcher translateMatcher = TRANSLATE_PATTERN.matcher(trimmed);
        if (translateMatcher.find()) {
            return new ParsedCommand(CommandType.TRANSLATE, null, null,
                    translateMatcher.group(1).trim(), rawText);
        }

        if (GRAMMAR_PATTERN.matcher(trimmed).find()) {
            return new ParsedCommand(CommandType.GRAMMAR_FIX, null, null, null, rawText);
        }

        return new ParsedCommand(CommandType.PLAIN_CHAT, null, null, null, rawText);
    }
}