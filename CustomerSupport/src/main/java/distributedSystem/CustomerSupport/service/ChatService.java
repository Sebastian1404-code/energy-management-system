package distributedSystem.CustomerSupport.service;


import distributedSystem.CustomerSupport.Domain.*;
import distributedSystem.CustomerSupport.repository.*;
import distributedSystem.CustomerSupport.dto.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;

    public ConversationDto getOrCreateConversationForUser(String userId) {
        Optional<Conversation> existing = conversationRepo.findByUserId(userId);
        if (existing.isPresent()) {
            System.out.println("[ChatService] Found existing conversation for userId: " + userId);
            return toDto(existing.get());
        } else {
            Instant now = Instant.now();
            Conversation c = Conversation.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            conversationRepo.save(c);
            System.out.println("[ChatService] Created new conversation for userId: " + userId + ", id: " + c.getId());
            return toDto(c);
        }
    }


    public ConversationDto getConversationForUser(String userId) {
        Conversation c = conversationRepo.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("No conversation for user"));
        return toDto(c);
    }

    public List<ConversationDto> listConversationsForAdmin() {
        List<Conversation> all = conversationRepo.findAll();
        System.out.println("[ChatService] Listing all conversations for admin, count: " + all.size());
        return all.stream().map(this::toDto).toList();
    }

    public List<MessageDto> getMessages(UUID conversationId, String requesterId, Role requesterRole) {
        Conversation c = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found"));

        // Authorization
        if (requesterRole == Role.USER && !Objects.equals(c.getUserId(), requesterId)) {
            throw new SecurityException("Forbidden");
        }

        return messageRepo.findByConversationId(conversationId).stream().map(this::toDto).toList();
    }

    public MessageDto sendUserMessage(String userId, String content) {
        ConversationDto conv = getOrCreateConversationForUser(userId);

        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conv.id())
                .senderId(userId)
                .senderRole(Role.USER)
                .content(content)
                .createdAt(Instant.now())
                .build();

        messageRepo.save(msg);
        touchConversation(conv.id());
        return toDto(msg);
    }

    public MessageDto sendAdminMessage(String adminId, UUID conversationId, String content) {

        if (conversationId == null) {
            throw new IllegalArgumentException("conversationId must not be null for admin messages");
        }
        Conversation c = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found"));

        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(adminId)
                .senderRole(Role.ADMIN)
                .content(content)
                .createdAt(Instant.now())
                .build();

        messageRepo.save(msg);
        c.setUpdatedAt(Instant.now());
        conversationRepo.save(c);

        return toDto(msg);
    }

    public Optional<String> getConversationOwnerUserId(UUID conversationId) {
        return conversationRepo.findById(conversationId).map(Conversation::getUserId);
    }

    private void touchConversation(UUID conversationId) {
        conversationRepo.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(Instant.now());
            conversationRepo.save(c);
        });
    }

    private ConversationDto toDto(Conversation c) {
        return new ConversationDto(c.getId(), c.getUserId(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private MessageDto toDto(Message m) {
        return new MessageDto(m.getId(), m.getConversationId(), m.getSenderId(), m.getSenderRole(), m.getContent(), m.getCreatedAt());
    }
}
