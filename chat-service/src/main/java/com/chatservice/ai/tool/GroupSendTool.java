package com.chatservice.ai.tool;

import com.chatservice.ai.dto.AiDtos.GroupCommandResult;
import com.chatservice.ai.exception.AiExceptions.AmbiguousGroupNameException;
import com.chatservice.ai.exception.AiExceptions.GroupNotResolvedException;
import com.chatservice.dto.ChatDtos.GroupInfo;
import com.chatservice.dto.ChatDtos.MessageResponse;
import com.chatservice.dto.ChatDtos.SendGroupMessageRequest;
import com.chatservice.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves a natural-language group name to a real chatId (scoped strictly
 * to groups the calling user belongs to) and sends the message through the
 * EXACT same ChatService.sendGroupMessage(...) path ChatController uses.
 *
 * Deliberately depends only on ChatService's public interface - never on
 * GroupRepository, GroupMemberRepository, or ChatServiceImpl directly - so
 * this stays correct even if persistence details change, and stays trivially
 * extractable if the ai/ package ever moves to its own microservice (it
 * would become a Feign client call instead).
 */
@Component
public class GroupSendTool {

    private static final Logger log = LoggerFactory.getLogger(GroupSendTool.class);

    private final ChatService chatService;

    public GroupSendTool(ChatService chatService) {
        this.chatService = chatService;
    }

    public GroupCommandResult sendToGroup(UUID senderId, String senderUsername,
                                           String spokenGroupName, String messageContent) {
        GroupCommandResult result = new GroupCommandResult();

        if (messageContent == null || messageContent.isBlank()) {
            result.setSuccess(false);
            result.setMessage("I couldn't figure out what message you want to send.");
            return result;
        }

        List<GroupInfo> userGroups = chatService.getGroupsForUser(senderId);

        GroupInfo match = resolveGroup(userGroups, spokenGroupName);

        if (match == null) {
            result.setSuccess(false);
            result.setMessage("I couldn't find a group called \"" + spokenGroupName +
                    "\" among the groups you're a member of. Please check the name and try again.");
            return result;
        }

        try {
            SendGroupMessageRequest request = new SendGroupMessageRequest();
            request.setChatId(resolveChatId(senderId, match));
            request.setContent(messageContent);
            request.setMessageType("TEXT");

            MessageResponse sent = chatService.sendGroupMessage(senderId, senderUsername, request);

            result.setSuccess(true);
            result.setResolvedGroupName(match.getName());
            result.setChatId(request.getChatId());
            result.setSentMessageId(sent.getId());
            result.setMessage("Sent to \"" + match.getName() + "\": " + messageContent);
            log.info("AI group-send: user={} group={} chatId={} messageId={}",
                    senderId, match.getName(), request.getChatId(), sent.getId());

        } catch (Exception e) {
            log.error("AI group-send failed for user={} group={}: {}", senderId, spokenGroupName, e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("I found the group but couldn't send the message: " + e.getMessage());
        }

        return result;
    }

    private GroupInfo resolveGroup(List<GroupInfo> candidates, String spokenName) {
        String normalized = spokenName.trim().toLowerCase();

        // 1. Exact case-insensitive match
        List<GroupInfo> exact = candidates.stream()
                .filter(g -> g.getName() != null && g.getName().toLowerCase().equals(normalized))
                .collect(Collectors.toList());
        if (exact.size() == 1) return exact.get(0);
        if (exact.size() > 1) throw new AmbiguousGroupNameException("Multiple groups named \"" + spokenName + "\".");

        // 2. Contains match - fallback for partial mentions (e.g. "Dev Team" said
        //    when the real group is "Development Team")
        List<GroupInfo> contains = candidates.stream()
                .filter(g -> g.getName() != null &&
                        (g.getName().toLowerCase().contains(normalized) || normalized.contains(g.getName().toLowerCase())))
                .collect(Collectors.toList());
        if (contains.size() == 1) return contains.get(0);
        if (contains.size() > 1) throw new AmbiguousGroupNameException("Multiple groups match \"" + spokenName + "\": " +
                contains.stream().map(GroupInfo::getName).collect(Collectors.joining(", ")));

        return null;
    }

    private String resolveChatId(UUID userId, GroupInfo group) {
        return chatService.getChatsForUser(userId).stream()
                .filter(c -> c.getGroupInfo() != null && group.getGroupId().equals(c.getGroupInfo().getGroupId()))
                .findFirst()
                .map(c -> c.getChatId())
                .orElseThrow(() -> new GroupNotResolvedException(
                        "Resolved group \"" + group.getName() + "\" but could not find its chat."));
    }
}