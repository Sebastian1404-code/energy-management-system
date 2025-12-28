package distributedSystem.CustomerSupport.service;


import distributedSystem.CustomerSupport.Domain.*;
import distributedSystem.CustomerSupport.gemini.GeminiClient;
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
    private final RuleBasedResponder ruleBasedResponder;
    private final GeminiClient geminiClient;



    public ConversationDto getOrCreateConversationForUser(String userId) {
        Optional<Conversation> existing = conversationRepo.findByUserId(userId);
        if (existing.isPresent()) {
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

        List<Message> messages = messageRepo.findByConversationId(conversationId);

        return messages.stream().map(this::toDto).toList();
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

    public Optional<MessageDto> maybeCreateBotReply(UUID conversationId, String userText) {
        if (conversationId == null) return Optional.empty();

        Optional<String> reply = ruleBasedResponder.match(userText);
        if (reply.isEmpty()) return Optional.empty();

        Message botMsg = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId("bot")
                .senderRole(Role.ADMIN) // Option A: leaves UI as admin reply
                .content(reply.get())
                .createdAt(Instant.now())
                .build();

        messageRepo.save(botMsg);
        touchConversation(conversationId);

        return Optional.of(toDto(botMsg));
    }

    public Optional<MessageDto> maybeCreateAiReply(UUID conversationId, String userText) {
        if (conversationId == null) return Optional.empty();

        // Very small context: last 6 messages (optional)
        List<String> ctx = messageRepo.findByConversationId(conversationId).stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .skip(Math.max(0, messageRepo.findByConversationId(conversationId).size() - 6))
                .map(m -> m.getSenderRole() + ": " + m.getContent())
                .toList();

        Optional<String> reply = geminiClient.generateReply(userText, ctx);
        if (reply.isEmpty()) return Optional.empty();

        Message aiMsg = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId("ai")
                .senderRole(Role.ADMIN) // Option A: render like admin/support
                .content(reply.get())
                .createdAt(Instant.now())
                .build();

        messageRepo.save(aiMsg);
        touchConversation(conversationId);

        return Optional.of(toDto(aiMsg));
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
