package com.chatservice.service.impl;

import com.chatservice.config.UserServiceClient;
import com.chatservice.dto.ChatDtos.*;
import com.chatservice.entity.*;
import com.chatservice.entity.Message.MessageStatus;
import com.chatservice.entity.Message.MessageType;
import com.chatservice.entity.MediaAttachment.MediaType;
import com.chatservice.entity.GroupMember.Role;
import com.chatservice.entity.CallSession.CallStatus;
import com.chatservice.exception.ChatExceptions.*;
import com.chatservice.kafka.ChatKafkaProducer;
import com.chatservice.repository.*;
import com.chatservice.service.ChatService;
import com.chatservice.service.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.of("UTC"));

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MessageEditRepository messageEditRepository;
    private final MessageVisibilityRepository visibilityRepository;
    private final MediaAttachmentRepository mediaRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupEventRepository groupEventRepository;
    private final ArchivedChatRepository archivedChatRepository;
    private final ChatSearchIndexRepository searchIndexRepository;
    private final ChatSettingsRepository chatSettingsRepository;
    private final ChatKafkaProducer kafkaProducer;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisCacheService cacheService;
    private final UserServiceClient userServiceClient;

    public ChatServiceImpl(
            ChatRepository chatRepository,
            ChatParticipantRepository participantRepository,
            MessageRepository messageRepository,
            MessageEditRepository messageEditRepository,
            MessageVisibilityRepository visibilityRepository,
            MediaAttachmentRepository mediaRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupEventRepository groupEventRepository,
            ArchivedChatRepository archivedChatRepository,
            ChatSearchIndexRepository searchIndexRepository,
            ChatSettingsRepository chatSettingsRepository,
            ChatKafkaProducer kafkaProducer,
            SimpMessagingTemplate messagingTemplate,
            RedisCacheService cacheService,
            UserServiceClient userServiceClient) {

        this.chatRepository = chatRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.messageEditRepository = messageEditRepository;
        this.visibilityRepository = visibilityRepository;
        this.mediaRepository = mediaRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupEventRepository = groupEventRepository;
        this.archivedChatRepository = archivedChatRepository;
        this.searchIndexRepository = searchIndexRepository;
        this.chatSettingsRepository = chatSettingsRepository;
        this.kafkaProducer = kafkaProducer;
        this.messagingTemplate = messagingTemplate;
        this.cacheService = cacheService;
        this.userServiceClient = userServiceClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send Private Message
    // ─────────────────────────────────────────────────────────────────────────
@Override
@Transactional
public MessageResponse sendPrivateMessage(
        UUID senderId,
        String senderUsername,
        SendPrivateMessageRequest request
) {

    UUID receiverId =
            userServiceClient.getUserIdByUsername(
                    request.getReceiverUsername()
            );

    log.info(
            "sendPrivateMessage sender={} receiver={}",
            senderId,
            receiverId
    );

    UUID chatId =
            findOrCreatePrivateChat(
                    senderId,
                    senderUsername,
                    receiverId,
                    request.getReceiverUsername()
            );

    // CONTACT
    if (
            "CONTACT".equalsIgnoreCase(
                    request.getMessageType()
            ) &&
            request.getContact() != null
    ) {

        return sendContact(
                senderId,
                senderUsername,
                chatId,
                request.getContact()
        );
    }

    Message message =
            buildMessage(
                    chatId,
                    senderId,
                    senderUsername,
                    request.getContent(),
                    request.getMessageType(),
                    request.getReplyToId()
            );

    // STICKER
    if (
            "STICKER".equalsIgnoreCase(
                    request.getMessageType()
            ) &&
            request.getStickerUrl() != null
    ) {

        message.setContent(
                request.getStickerUrl()
        );
    }

    message =
            messageRepository.save(
                    message
            );

    MediaAttachment attachment = null;

    if (
            hasAttachment(
                    request.getMessageType()
            )
    ) {

        attachment =
                saveAttachment(
                        message.getId(),
                        chatId,
                        request.getMessageType(),
                        request.getFileData(),
                        request.getFileName(),
                        request.getFileType(),
                        request.getFileSizeBytes(),
                        request.getUrl(),
                        request.getPreviewTitle(),
                        request.getPreviewDesc(),
                        request.getStickerId()
                );
    }

    indexMessage(
            message,
            attachment
    );

    cacheService.evictChatMessages(
            chatId.toString()
    );

    kafkaProducer.publishMessageSent(
            chatId.toString(),
            message.getId().toString(),
            senderId.toString()
    );

    // ✅ FORCE DELIVERED INSTANTLY
    message.setStatus(
            MessageStatus.DELIVERED
    );

    message.setDeliveredAt(
            Instant.now()
    );

    message =
            messageRepository.save(
                    message
            );

    MessageResponse response =
            toMessageResponse(
                    message,
                    attachment
            );

    // ✅ SEND REALTIME MESSAGE
   WsMessage wsMessage = buildWsMessage(response);

// ✅ Current open chat realtime
messagingTemplate.convertAndSend(
        "/topic/chat/" + chatId,
        wsMessage
);

// ✅ Receiver personal inbox realtime
messagingTemplate.convertAndSendToUser(
        receiverId.toString(),
        "/queue/messages",
        wsMessage
);

    // ✅ SEND REALTIME TICK UPDATE
    Map<String, Object> ws =
            new HashMap<>();

    ws.put(
            "type",
            "STATUS_UPDATE"
    );

    ws.put(
            "messageId",
            message.getId().toString()
    );

    ws.put(
            "status",
            "DELIVERED"
    );

    ws.put(
            "chatId",
            chatId.toString()
    );

   messagingTemplate.convertAndSendToUser(
        receiverId.toString(),
        "/queue/messages",
        ws
);

    log.info(
            "Private realtime message {} sent in chat {}",
            message.getId(),
            chatId
    );

    return response;
}
    // ─────────────────────────────────────────────────────────────────────────
    // Get Chats for User by Username
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<ChatSummaryResponse> getChatsForUserByUsername(String username) {
        log.info("getChatsForUserByUsername username={}", username);
        UUID userId = userServiceClient.getUserIdByUsername(username);
        return getChatsForUser(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send Group Message
    // ─────────────────────────────────────────────────────────────────────────
@Override
@Transactional
public MessageResponse sendGroupMessage(
        UUID senderId,
        String senderUsername,
        SendGroupMessageRequest request
) {

    UUID chatId =
            UUID.fromString(
                    request.getChatId()
            );

    if (
            !participantRepository
                    .existsByChatIdAndUserId(
                            chatId,
                            senderId
                    )
    ) {

        throw new NotChatMemberException(
                "You are not a member of this group."
        );
    }

    log.info(
            "sendGroupMessage sender={} chatId={}",
            senderId,
            chatId
    );

    // CONTACT
    if (
            "CONTACT".equalsIgnoreCase(
                    request.getMessageType()
            ) &&
            request.getContact() != null
    ) {

        return sendContact(
                senderId,
                senderUsername,
                chatId,
                request.getContact()
        );
    }

    Message message =
            buildMessage(
                    chatId,
                    senderId,
                    senderUsername,
                    request.getContent(),
                    request.getMessageType(),
                    request.getReplyToId()
            );

    // STICKER
    if (
            "STICKER".equalsIgnoreCase(
                    request.getMessageType()
            ) &&
            request.getStickerUrl() != null
    ) {

        message.setContent(
                request.getStickerUrl()
        );
    }

    message =
            messageRepository.save(
                    message
            );

    MediaAttachment attachment = null;

    if (
            hasAttachment(
                    request.getMessageType()
            )
    ) {

        attachment =
                saveAttachment(
                        message.getId(),
                        chatId,
                        request.getMessageType(),
                        request.getFileData(),
                        request.getFileName(),
                        request.getFileType(),
                        request.getFileSizeBytes(),
                        request.getUrl(),
                        request.getPreviewTitle(),
                        request.getPreviewDesc(),
                        request.getStickerId()
                );
    }

    indexMessage(
            message,
            attachment
    );

    cacheService.evictChatMessages(
            chatId.toString()
    );

    kafkaProducer.publishMessageSent(
            chatId.toString(),
            message.getId().toString(),
            senderId.toString()
    );

    // ✅ FORCE DELIVERED
    message.setStatus(
            MessageStatus.DELIVERED
    );

    message.setDeliveredAt(
            Instant.now()
    );

    message =
            messageRepository.save(
                    message
            );

    MessageResponse response =
            toMessageResponse(
                    message,
                    attachment
            );

    // ✅ SEND REALTIME MESSAGE
    messagingTemplate.convertAndSend(
            "/topic/chat/" + chatId,
            buildWsMessage(response)
    );

    // ✅ SEND REALTIME STATUS UPDATE
    Map<String, Object> ws =
            new HashMap<>();

    ws.put(
            "type",
            "STATUS_UPDATE"
    );

    ws.put(
            "messageId",
            message.getId().toString()
    );

    ws.put(
            "status",
            "DELIVERED"
    );

    ws.put(
            "chatId",
            chatId.toString()
    );

    messagingTemplate.convertAndSend(
            "/topic/chat/" + chatId,
            ws
    );

    log.info(
            "Group realtime message {} sent in chat {}",
            message.getId(),
            chatId
    );

    return response;
}


    // ─────────────────────────────────────────────────────────────────────────
    // Get Chats for User
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<ChatSummaryResponse> getChatsForUser(UUID userId) {
        log.info("getChatsForUser userId={}", userId);

        Set<UUID> archivedChatIds = archivedChatRepository.findByUserId(userId)
                .stream().map(ArchivedChat::getChatId).collect(Collectors.toSet());

        return participantRepository.findByUserId(userId).stream()
                .filter(p -> p.getLeftAt() == null)
                .filter(p -> !archivedChatIds.contains(p.getChatId()))
                .map(p -> buildChatSummary(p.getChatId(), userId, false))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Chat Messages — FIX: no N+1, uses bulk attachment load
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<MessageResponse> getChatMessages(UUID chatId, UUID requestingUserId) {
        log.info("getChatMessages chatId={} userId={}", chatId, requestingUserId);

        // Use per-user filtered query — replaces old findByChatIdOrdered +
        // filterDeletedForMe
        List<Message> messages = messageRepository.findByChatIdOrderedForUser(chatId, requestingUserId);

        if (messages.isEmpty())
            return Collections.emptyList();

        // Batch load all attachments — avoids N+1 DB calls per message
        List<UUID> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());
        Map<UUID, MediaAttachment> attachmentMap = mediaRepository
                .findByMessageIds(messageIds)
                .stream()
                .collect(Collectors.toMap(MediaAttachment::getMessageId, a -> a,
                        (a1, a2) -> a1)); // keep first if multiple

        return messages.stream()
                .map(m -> toMessageResponse(m, attachmentMap.get(m.getId())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Group
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public GroupInfo createGroup(UUID creatorId, String creatorUsername,
            CreateGroupRequest request) {
        log.info("createGroup creator={} name={}", creatorId, request.getName());

        Chat chat = new Chat();
        chat.setType(Chat.ChatType.GROUP);
        chat = chatRepository.save(chat);
        final UUID chatId = chat.getId();

        Group group = new Group();
        group.setChatId(chatId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCreatorId(creatorId);
        group.setProfilePicture(request.getProfilePicture());
        group.setMemberCount(1);
        group = groupRepository.save(group);
        final UUID groupId = group.getId();

        addParticipant(chatId, creatorId, creatorUsername);
        addGroupMember(groupId, creatorId, creatorUsername, Role.ADMIN);

        if (request.getMemberIds() != null) {
            for (String memberId : request.getMemberIds()) {
                UUID memberUuid = UUID.fromString(memberId);
                if (!memberUuid.equals(creatorId)) {
                    addParticipant(chatId, memberUuid, null);
                    addGroupMember(groupId, memberUuid, null, Role.MEMBER);
                    group.setMemberCount(group.getMemberCount() + 1);
                }
            }
        }

        group = groupRepository.save(group);
        recordGroupEvent(groupId, "GROUP_CREATED", creatorId, null,
                "Group '" + request.getName() + "' created by " + creatorUsername);

        log.info("Group {} created with chatId={}", groupId, chatId);
        return toGroupInfo(group);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update Group Info (req #21)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public GroupInfo updateGroupInfo(UUID groupId, UUID adminId, UpdateGroupRequest request) {
        assertAdmin(groupId, adminId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group not found."));

        if (request.getName() != null && !request.getName().isBlank()) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getProfilePicture() != null) {
            group.setProfilePicture(request.getProfilePicture());
        }

        group = groupRepository.save(group);

        recordGroupEvent(groupId, "GROUP_UPDATED", adminId, null,
                "Group info updated by admin");

        log.info("Group {} info updated by admin {}", groupId, adminId);
        return toGroupInfo(group);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Group Events (req #22)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<GroupEventResponse> getGroupEvents(UUID groupId) {
        return groupEventRepository.findByGroupIdOrderByOccurredAtAsc(groupId)
                .stream()
                .map(this::toGroupEventResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Groups for User
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<GroupInfo> getGroupsForUser(UUID userId) {
        return groupMemberRepository.findByUserIdAndLeftAtIsNull(userId).stream()
                .map(gm -> groupRepository.findById(gm.getGroupId())
                        .map(this::toGroupInfo).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add Member
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void addMember(UUID groupId, UUID adminId, UUID newMemberId, String newMemberUsername) {
        assertAdmin(groupId, adminId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group not found."));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, newMemberId)) {
            throw new AlreadyMemberException("User is already a member.");
        }

        addParticipant(group.getChatId(), newMemberId, newMemberUsername);
        addGroupMember(groupId, newMemberId, newMemberUsername, Role.MEMBER);

        // Sync memberCount atomically
        group.setMemberCount(group.getMemberCount() + 1);
        groupRepository.save(group);

        recordGroupEvent(groupId, "MEMBER_JOINED", adminId, newMemberId, "Member added by admin");
        sendGroupEventMessage(group.getChatId(), groupId, adminId,
                (newMemberUsername != null ? newMemberUsername : newMemberId.toString())
                        + " was added to the group");

        kafkaProducer.publishGroupEvent(groupId.toString(), "MEMBER_JOINED",
                adminId.toString(), newMemberId.toString());

        log.info("Member {} added to group {} by admin {}", newMemberId, groupId, adminId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Remove Member
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void removeMember(UUID groupId, UUID adminId, UUID targetUserId) {
        assertAdmin(groupId, adminId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group not found."));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new NotChatMemberException("User is not a member."));

        member.setLeftAt(Instant.now());
        groupMemberRepository.save(member);

        // Sync memberCount atomically
        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        groupRepository.save(group);

        recordGroupEvent(groupId, "MEMBER_REMOVED", adminId, targetUserId, "Member removed by admin");
        sendGroupEventMessage(group.getChatId(), groupId, adminId, "A member was removed from the group");

        kafkaProducer.publishGroupEvent(groupId.toString(), "MEMBER_REMOVED",
                adminId.toString(), targetUserId.toString());

        log.info("Member {} removed from group {} by admin {}", targetUserId, groupId, adminId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Promote Admin
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void promoteAdmin(UUID groupId, UUID adminId, UUID targetUserId) {
        assertAdmin(groupId, adminId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new NotChatMemberException("User is not a member."));

        member.setRole(Role.ADMIN);
        groupMemberRepository.save(member);

        recordGroupEvent(groupId, "ADMIN_CHANGED", adminId, targetUserId, "User promoted to admin");
        groupRepository.findById(groupId).ifPresent(group -> sendGroupEventMessage(group.getChatId(), groupId, adminId,
                "A member was promoted to admin"));

        log.info("User {} promoted to admin in group {} by {}", targetUserId, groupId, adminId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Group Members
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<GroupMemberResponse> getGroupMembers(UUID groupId) {
        return groupMemberRepository.findByGroupIdAndLeftAtIsNull(groupId).stream()
                .map(this::toGroupMemberResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit Message
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public MessageResponse editMessage(UUID messageId, UUID editorId, EditMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found."));

        if (!message.getSenderId().equals(editorId)) {
            throw new UnauthorizedMessageActionException("You can only edit your own messages.");
        }
        if (message.isDeleted()) {
            throw new UnauthorizedMessageActionException("Cannot edit a deleted message.");
        }

        MessageEdit edit = new MessageEdit();
        edit.setMessageId(messageId);
        edit.setOriginalContent(message.getContent());
        edit.setEditedContent(request.getContent());
        edit.setEditedBy(editorId);
        messageEditRepository.save(edit);

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        message = messageRepository.save(message);

        cacheService.evictChatMessages(message.getChatId().toString());

        MessageResponse response = toMessageResponse(message, null);
        WsMessage wsMsg = buildWsMessage(response);
        wsMsg.setType("EDIT");
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId(), wsMsg);

        log.info("Message {} edited by {}", messageId, editorId);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete For Me
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteForMe(UUID messageId, UUID userId) {
        if (!messageRepository.existsById(messageId)) {
            throw new MessageNotFoundException("Message not found.");
        }
        if (visibilityRepository.existsByMessageIdAndUserId(messageId, userId))
            return;

        MessageVisibility vis = new MessageVisibility();
        vis.setMessageId(messageId);
        vis.setUserId(userId);
        visibilityRepository.save(vis);

        log.info("Message {} hidden for user {} (Delete for Me)", messageId, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete For Everyone
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteForEveryone(UUID messageId, UUID requesterId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found."));

        if (!message.getSenderId().equals(requesterId)) {
            throw new UnauthorizedMessageActionException(
                    "You can only delete your own messages for everyone.");
        }

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        messageRepository.save(message);

        cacheService.evictChatMessages(message.getChatId().toString());

        WsMessage wsMsg = new WsMessage();
        wsMsg.setType("DELETE");
        wsMsg.setMessageId(messageId.toString());
        wsMsg.setChatId(message.getChatId().toString());
        wsMsg.setDeleted(true);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId(), wsMsg);

        log.info("Message {} deleted for everyone by {}", messageId, requesterId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Media retrieval
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<MediaAttachmentResponse> getChatImages(UUID chatId) {
        return mediaRepository.findByChatIdAndMediaTypeOrderByUploadedAtDesc(chatId, MediaType.IMAGE)
                .stream().map(this::toMediaResponse).collect(Collectors.toList());
    }

    @Override
    public List<MediaAttachmentResponse> getChatFiles(UUID chatId) {
        return mediaRepository.findByChatIdAndMediaTypeOrderByUploadedAtDesc(chatId, MediaType.FILE)
                .stream().map(this::toMediaResponse).collect(Collectors.toList());
    }

    @Override
    public List<MediaAttachmentResponse> getChatLinks(UUID chatId) {
        return mediaRepository.findByChatIdAndMediaTypeOrderByUploadedAtDesc(chatId, MediaType.LINK)
                .stream().map(this::toMediaResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search (req #29) — basic text search
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<MessageResponse> searchChat(UUID chatId, String query, UUID requestingUserId) {
        log.info("searchChat chatId={} query={}", chatId, query);

        Set<UUID> hiddenIds = visibilityRepository.findHiddenMessageIdsByUserId(requestingUserId);

        return searchIndexRepository.searchInChat(chatId, query).stream()
                .filter(idx -> !hiddenIds.contains(idx.getMessageId()))
                .map(idx -> messageRepository.findById(idx.getMessageId()).map(m -> {
                    List<MediaAttachment> atts = mediaRepository.findByMessageId(m.getId());
                    return toMessageResponse(m, atts.isEmpty() ? null : atts.get(0));
                }).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search with filters (req #29) — by sender, date, mediaType
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<MessageResponse> searchChatWithFilters(UUID chatId, UUID requestingUserId,
            String query, UUID senderId,
            String mediaType,
            Instant from, Instant to) {
        log.info("searchChatWithFilters chatId={} query={} senderId={} mediaType={}",
                chatId, query, senderId, mediaType);

        Set<UUID> hiddenIds = visibilityRepository.findHiddenMessageIdsByUserId(requestingUserId);

        return searchIndexRepository.searchWithFilters(chatId, query, senderId, mediaType, from, to)
                .stream()
                .filter(idx -> !hiddenIds.contains(idx.getMessageId()))
                .map(idx -> messageRepository.findById(idx.getMessageId()).map(m -> {
                    List<MediaAttachment> atts = mediaRepository.findByMessageId(m.getId());
                    return toMessageResponse(m, atts.isEmpty() ? null : atts.get(0));
                }).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Archive
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void archiveChat(UUID chatId, UUID userId) {
        if (archivedChatRepository.existsByChatIdAndUserId(chatId, userId))
            return;

        ArchivedChat arc = new ArchivedChat();
        arc.setChatId(chatId);
        arc.setUserId(userId);
        archivedChatRepository.save(arc);

        kafkaProducer.publishChatArchived(chatId.toString(), userId.toString());
        log.info("Chat {} archived by {}", chatId, userId);
    }

    @Override
    @Transactional
    public void unarchiveChat(UUID chatId, UUID userId) {
        archivedChatRepository.findByChatIdAndUserId(chatId, userId)
                .ifPresent(archivedChatRepository::delete);
        log.info("Chat {} unarchived by {}", chatId, userId);
    }

    @Override
    public List<ChatSummaryResponse> getArchivedChats(UUID userId) {
        return archivedChatRepository.findByUserId(userId).stream()
                .map(a -> buildChatSummary(a.getChatId(), userId, true))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search in Archived Chats (req #25)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<MessageResponse> searchArchivedChats(UUID userId, String query) {
        log.info("searchArchivedChats userId={} query={}", userId, query);

        List<UUID> archivedChatIds = archivedChatRepository.findByUserId(userId)
                .stream().map(ArchivedChat::getChatId).collect(Collectors.toList());

        if (archivedChatIds.isEmpty())
            return Collections.emptyList();

        Set<UUID> hiddenIds = visibilityRepository.findHiddenMessageIdsByUserId(userId);

        return searchIndexRepository.searchInArchivedChats(archivedChatIds, query).stream()
                .filter(idx -> !hiddenIds.contains(idx.getMessageId()))
                .map(idx -> messageRepository.findById(idx.getMessageId()).map(m -> {
                    List<MediaAttachment> atts = mediaRepository.findByMessageId(m.getId());
                    return toMessageResponse(m, atts.isEmpty() ? null : atts.get(0));
                }).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void markDelivered(UUID chatId, UUID userId) {
        List<Message> unread = messageRepository.findUnreadMessages(chatId, MessageStatus.SENT, userId);
        Instant now = Instant.now();
        for (Message m : unread) {

    m.setStatus(MessageStatus.DELIVERED);

    m.setDeliveredAt(now);

    messageRepository.save(m);

    // ✅ REALTIME WS STATUS UPDATE
    Map<String, Object> ws = new HashMap<>();

    ws.put("type", "STATUS_UPDATE");

    ws.put("messageId", m.getId().toString());

    ws.put("status", "DELIVERED");

    ws.put("chatId", chatId.toString());

    messagingTemplate.convertAndSend(
        "/topic/chat/" + chatId,
        ws
    );

    kafkaProducer.publishMessageDelivered(
        chatId.toString(),
        m.getId().toString()
    );
}
        log.info("Marked {} messages DELIVERED in chat {} for user {}", unread.size(), chatId, userId);
    }

    @Override
    @Transactional
    public void markRead(UUID chatId, UUID userId) {
        List<Message> messages = messageRepository.findUnreadMessages(chatId, MessageStatus.DELIVERED, userId);
        Instant now = Instant.now();
       for (Message m : messages) {

    m.setStatus(MessageStatus.READ);

    m.setReadAt(now);

    messageRepository.save(m);

    // ✅ REALTIME WS STATUS UPDATE
    Map<String, Object> ws = new HashMap<>();

    ws.put("type", "STATUS_UPDATE");

    ws.put("messageId", m.getId().toString());

    ws.put("status", "READ");

    ws.put("chatId", chatId.toString());

    messagingTemplate.convertAndSend(
        "/topic/chat/" + chatId,
        ws
    );

    kafkaProducer.publishMessageRead(
        chatId.toString(),
        m.getId().toString(),
        userId.toString()
    );
}
        cacheService.evictChatMessages(chatId.toString());
        log.info("Marked {} messages READ in chat {} for user {}", messages.size(), chatId, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wallpaper
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public WallpaperResponse setWallpaper(UUID chatId, UUID userId, WallpaperRequest request) {
        log.info("setWallpaper chatId={} userId={}", chatId, userId);

        ChatSettings settings = chatSettingsRepository.findByChatIdAndUserId(chatId, userId)
                .orElseGet(() -> {
                    ChatSettings s = new ChatSettings();
                    s.setChatId(chatId);
                    s.setUserId(userId);
                    return s;
                });

        settings.setWallpaperType(request.getWallpaperType());
        settings.setWallpaperData(request.getWallpaperData());
        settings.setWallpaperColor(request.getWallpaperColor());
        settings = chatSettingsRepository.save(settings);

        return toWallpaperResponse(settings);
    }

    @Override
    public WallpaperResponse getWallpaper(UUID chatId, UUID userId) {
        return chatSettingsRepository.findByChatIdAndUserId(chatId, userId)
                .map(this::toWallpaperResponse)
                .orElseGet(() -> {
                    WallpaperResponse r = new WallpaperResponse();
                    r.setChatId(chatId.toString());
                    r.setUserId(userId.toString());
                    r.setWallpaperType("DEFAULT");
                    return r;
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contact Sharing
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public MessageResponse sendContact(UUID senderId, String senderUsername,
            UUID chatId, ContactPayload contact) {
        log.info("sendContact sender={} chatId={}", senderId, chatId);

        if (!participantRepository.existsByChatIdAndUserId(chatId, senderId)) {
            throw new NotChatMemberException("You are not a member of this chat.");
        }

        String contactJson;
        try {
            contactJson = new ObjectMapper().writeValueAsString(contact);
        } catch (Exception e) {
            contactJson = contact.getName() + " - " + contact.getPhoneNumber();
        }

        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setSenderUsername(senderUsername);
        message.setContent(contactJson);
        message.setMessageType(MessageType.CONTACT);
        message.setStatus(MessageStatus.SENT);
        message = messageRepository.save(message);

        MediaAttachment att = new MediaAttachment();
        att.setMessageId(message.getId());
        att.setChatId(chatId);
        att.setMediaType(MediaType.CONTACT);
        att.setFileName(contact.getName());
        att.setPayload(contactJson);
        mediaRepository.save(att);

        indexMessage(message, att);
        cacheService.evictChatMessages(chatId.toString());
        kafkaProducer.publishMessageSent(chatId.toString(), message.getId().toString(),
                senderId.toString());

        MessageResponse response = toMessageResponse(message, att);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, buildWsMessage(response));

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private UUID findOrCreatePrivateChat(UUID userA, String usernameA,
            UUID userB, String usernameB) {
        List<UUID> shared = participantRepository.findPrivateChatBetween(userA, userB);
        if (!shared.isEmpty())
            return shared.get(0);

        Chat chat = new Chat();
        chat.setType(Chat.ChatType.PRIVATE);
        chat = chatRepository.save(chat);

        addParticipant(chat.getId(), userA, usernameA);
        addParticipant(chat.getId(), userB, usernameB);

        log.info("Created new private chat {} between {} and {}", chat.getId(), userA, userB);
        return chat.getId();
    }

    private void addParticipant(UUID chatId, UUID userId, String username) {
        if (!participantRepository.existsByChatIdAndUserId(chatId, userId)) {
            ChatParticipant p = new ChatParticipant();
            p.setChatId(chatId);
            p.setUserId(userId);
            p.setUsername(username);
            participantRepository.save(p);
        }
    }

    private void addGroupMember(UUID groupId, UUID userId, String username, Role role) {
        GroupMember gm = new GroupMember();
        gm.setGroupId(groupId);
        gm.setUserId(userId);
        gm.setUsername(username);
        gm.setRole(role);
        groupMemberRepository.save(gm);
    }

    private void recordGroupEvent(UUID groupId, String eventType, UUID actorId,
            UUID targetId, String description) {
        GroupEvent event = new GroupEvent();
        event.setGroupId(groupId);
        event.setEventType(eventType);
        event.setActorId(actorId);
        event.setTargetId(targetId);
        event.setDescription(description);
        groupEventRepository.save(event);
    }

    private void assertAdmin(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, Role.ADMIN)) {
            throw new NotGroupAdminException("Only admins can perform this action.");
        }
    }

    private Message buildMessage(UUID chatId, UUID senderId, String senderUsername,
            String content, String messageTypeStr, String replyToId) {
        Message m = new Message();
        m.setChatId(chatId);
        m.setSenderId(senderId);
        m.setSenderUsername(senderUsername);
        m.setContent(content);
        m.setStatus(MessageStatus.SENT);

        try {
            m.setMessageType(MessageType.valueOf(messageTypeStr.toUpperCase()));
        } catch (Exception e) {
            m.setMessageType(MessageType.TEXT);
        }

        if (replyToId != null) {
            try {
                m.setReplyToId(UUID.fromString(replyToId));
            } catch (Exception ignored) {
            }
        }
        return m;
    }

    private MediaAttachment saveAttachment(UUID messageId, UUID chatId, String messageType,
            String fileData, String fileName, String fileType, Long fileSizeBytes,
            String url, String previewTitle, String previewDesc, String stickerId) {

        MediaAttachment att = new MediaAttachment();
        att.setMessageId(messageId);
        att.setChatId(chatId);
        att.setFileName(fileName != null ? fileName : stickerId);
        att.setFileType(fileType);
        att.setFileSizeBytes(fileSizeBytes);
        // Store file path / sticker ID, NOT raw Base64
        att.setStoragePath(fileData != null ? fileData : stickerId);
        att.setUrl(url);
        att.setPreviewTitle(previewTitle);
        att.setPreviewDesc(previewDesc);

        switch (messageType.toUpperCase()) {
            case "IMAGE" -> att.setMediaType(MediaType.IMAGE);
            case "FILE" -> att.setMediaType(MediaType.FILE);
            case "CONTACT" -> att.setMediaType(MediaType.CONTACT);
            case "STICKER" -> att.setMediaType(MediaType.STICKER);
            case "LINK" -> att.setMediaType(MediaType.LINK);
            default -> att.setMediaType(MediaType.FILE);
        }
        return mediaRepository.save(att);
    }

    private void indexMessage(Message message, MediaAttachment attachment) {
        ChatSearchIndex idx = new ChatSearchIndex();
        idx.setChatId(message.getChatId());
        idx.setMessageId(message.getId());
        idx.setSenderId(message.getSenderId());
        idx.setSenderUsername(message.getSenderUsername());
        idx.setContent(message.getContent());
        idx.setSentAt(message.getSentAt() != null ? message.getSentAt() : Instant.now());
        idx.setMessageDate(LocalDate.ofInstant(
                message.getSentAt() != null ? message.getSentAt() : Instant.now(),
                ZoneId.of("UTC")));

        if (attachment != null) {
            idx.setFileName(attachment.getFileName());
            idx.setUrl(attachment.getUrl());
            idx.setMediaType(attachment.getMediaType() != null
                    ? attachment.getMediaType().name()
                    : null);
        }
        searchIndexRepository.save(idx);
    }

    private boolean hasAttachment(String messageType) {
        if (messageType == null)
            return false;
        return switch (messageType.toUpperCase()) {
            case "IMAGE", "FILE", "CONTACT", "STICKER", "LINK" -> true;
            default -> false;
        };
    }

    private ChatSummaryResponse buildChatSummary(UUID chatId, UUID userId, boolean archived) {
        ChatSummaryResponse summary = new ChatSummaryResponse();
        summary.setChatId(chatId.toString());
        summary.setArchived(archived);

        chatRepository.findById(chatId).ifPresent(chat -> {
            summary.setType(chat.getType().name());

            if (chat.getType() == Chat.ChatType.GROUP) {
                groupRepository.findByChatId(chatId)
                        .ifPresent(group -> summary.setGroupInfo(toGroupInfo(group)));
            } else {
                // Set otherParticipantUsername for private chats
                participantRepository.findByChatId(chatId).stream()
                        .filter(p -> !p.getUserId().equals(userId))
                        .findFirst()
                        .ifPresent(p -> summary.setOtherParticipantUsername(p.getUsername()));
            }
        });

        List<Message> recent = messageRepository.findRecentMessages(chatId, 1);
        if (!recent.isEmpty()) {
            Message last = recent.get(0);
            summary.setLastMessage(last.getContent());
            summary.setLastMessageAt(last.getSentAt().toString());
        }

        // Set unreadCount
        long unread = messageRepository.findUnreadMessages(chatId, MessageStatus.SENT, userId).size()
                + messageRepository.findUnreadMessages(chatId, MessageStatus.DELIVERED, userId).size();
        summary.setUnreadCount((int) unread);

        return summary;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────────────────────────────────

    private MessageResponse toMessageResponse(Message m, MediaAttachment attachment) {
        MessageResponse r = new MessageResponse();
        r.setId(m.getId().toString());
        r.setChatId(m.getChatId().toString());
        r.setSenderId(m.getSenderId().toString());
        r.setSenderUsername(m.getSenderUsername());
        r.setMessageType(m.getMessageType().name());
        r.setContent(m.getContent());
        r.setStatus(m.getStatus().name());
        r.setEdited(m.isEdited());
        r.setDeleted(m.isDeleted());
        r.setReplyToId(m.getReplyToId() != null ? m.getReplyToId().toString() : null);
        r.setSentAt(m.getSentAt() != null ? m.getSentAt().toString() : null);
        r.setDeliveredAt(m.getDeliveredAt() != null ? m.getDeliveredAt().toString() : null);
        r.setReadAt(m.getReadAt() != null ? m.getReadAt().toString() : null);
        r.setEditedAt(m.getEditedAt() != null ? m.getEditedAt().toString() : null);
        r.setDeletedAt(m.getDeletedAt() != null ? m.getDeletedAt().toString() : null);
        r.setUpdatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
        r.setDate(m.getSentAt() != null ? DATE_FORMATTER.format(m.getSentAt()) : null);
        if (attachment != null)
            r.setAttachment(toMediaResponse(attachment));
        return r;
    }

    private MediaAttachmentResponse toMediaResponse(MediaAttachment a) {
        MediaAttachmentResponse r = new MediaAttachmentResponse();
        r.setId(a.getId().toString());
        r.setFileName(a.getFileName());
        r.setFileType(a.getFileType());
        r.setFileSizeBytes(a.getFileSizeBytes());
        r.setMediaType(a.getMediaType() != null ? a.getMediaType().name() : null);
        r.setStoragePath(a.getStoragePath());
        r.setUrl(a.getUrl());
        r.setPreviewTitle(a.getPreviewTitle());
        r.setPreviewDesc(a.getPreviewDesc());
        r.setUploadedAt(a.getUploadedAt() != null ? a.getUploadedAt().toString() : null);
        return r;
    }

    private GroupInfo toGroupInfo(Group g) {
        GroupInfo info = new GroupInfo();
        info.setGroupId(g.getId().toString());
        info.setName(g.getName());
        info.setDescription(g.getDescription());
        info.setProfilePicture(g.getProfilePicture());
        info.setMemberCount(g.getMemberCount());
        info.setCreatorId(g.getCreatorId().toString());
        info.setCreatedAt(g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
        return info;
    }

    private GroupMemberResponse toGroupMemberResponse(GroupMember gm) {
        GroupMemberResponse r = new GroupMemberResponse();
        r.setUserId(gm.getUserId().toString());
        r.setUsername(gm.getUsername());
        r.setRole(gm.getRole().name());
        r.setJoinedAt(gm.getJoinedAt() != null ? gm.getJoinedAt().toString() : null);
        r.setLeftAt(gm.getLeftAt() != null ? gm.getLeftAt().toString() : null);
        return r;
    }

    private GroupEventResponse toGroupEventResponse(GroupEvent e) {
        GroupEventResponse r = new GroupEventResponse();
        r.setEventId(e.getId().toString());
        r.setGroupId(e.getGroupId().toString());
        r.setEventType(e.getEventType());
        r.setActorId(e.getActorId() != null ? e.getActorId().toString() : null);
        r.setTargetId(e.getTargetId() != null ? e.getTargetId().toString() : null);
        r.setDescription(e.getDescription());
        r.setOccurredAt(e.getOccurredAt() != null ? e.getOccurredAt().toString() : null);
        return r;
    }

    private WsMessage buildWsMessage(MessageResponse r) {
        WsMessage ws = new WsMessage();
        ws.setType("MESSAGE");
        ws.setMessageId(r.getId());
        ws.setChatId(r.getChatId());
        ws.setSenderId(r.getSenderId());
        ws.setSenderUsername(r.getSenderUsername());
        ws.setContent(r.getContent());
        ws.setMessageType(r.getMessageType());
        ws.setStatus(r.getStatus());
        ws.setSentAt(r.getSentAt());
        ws.setEdited(r.isEdited());
        ws.setDeleted(r.isDeleted());
        ws.setReplyToId(r.getReplyToId());
        ws.setDate(r.getDate());
        ws.setAttachment(r.getAttachment());
        return ws;
    }

    private void sendGroupEventMessage(UUID chatId, UUID groupId,
            UUID actorId, String eventText) {
        try {
            Message eventMsg = new Message();
            eventMsg.setChatId(chatId);
            eventMsg.setSenderId(actorId);
            eventMsg.setSenderUsername("System");
            eventMsg.setContent(eventText);
            eventMsg.setMessageType(MessageType.GROUP_EVENT);
            eventMsg.setStatus(MessageStatus.SENT);
            eventMsg = messageRepository.save(eventMsg);

            cacheService.evictChatMessages(chatId.toString());

            MessageResponse r = toMessageResponse(eventMsg, null);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, buildWsMessage(r));
            log.info("GROUP_EVENT message sent in chatId={}: {}", chatId, eventText);
        } catch (Exception e) {
            log.warn("Failed to send GROUP_EVENT message: {}", e.getMessage());
        }
    }

    private WallpaperResponse toWallpaperResponse(ChatSettings s) {
        WallpaperResponse r = new WallpaperResponse();
        r.setChatId(s.getChatId().toString());
        r.setUserId(s.getUserId().toString());
        r.setWallpaperType(s.getWallpaperType());
        r.setWallpaperData(s.getWallpaperData());
        r.setWallpaperColor(s.getWallpaperColor());
        r.setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        return r;
    }
}