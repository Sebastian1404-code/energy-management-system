package distributedSystem.CustomerSupport.controller;


import distributedSystem.CustomerSupport.service.ChatService;
import distributedSystem.CustomerSupport.dto.*;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messaging;

    // USER sends a message
    @MessageMapping("/chat.user.send")
    public void userSend(@Payload WsSend req,
                         @Header("simpSessionAttributes") Map<String, Object> attrs) {

        String userId = String.valueOf(attrs.getOrDefault("userId", "anonymous"));

        System.out.println("Received user message: " + req.content() + " from userId: " + userId);


        MessageDto saved = chatService.sendUserMessage(userId, req.content());

        // user thread
        messaging.convertAndSend("/topic/chat.user." + userId, saved);
        // admin inbox
        messaging.convertAndSend("/topic/chat.admin.inbox", saved);


        Optional<MessageDto> bot = chatService.maybeCreateBotReply(saved.conversationId(), req.content());
        if (bot.isPresent()) {
            MessageDto botMsg = bot.get();
            messaging.convertAndSend("/topic/chat.user." + userId, botMsg);
            messaging.convertAndSend("/topic/chat.admin.inbox", botMsg);
        } else {
            chatService.maybeCreateAiReply(saved.conversationId(), req.content())
                    .ifPresent(aiMsg -> {
                        messaging.convertAndSend("/topic/chat.user." + userId, aiMsg);
                        messaging.convertAndSend("/topic/chat.admin.inbox", aiMsg);
                    });
        }
    }

    @MessageMapping("/chat.admin.send")
    public void adminSend(@Payload WsAdminSend req,
                          @Header("simpSessionAttributes") Map<String, Object> attrs) {

        String adminId = String.valueOf(attrs.getOrDefault("userId", "admin"));
        String role = String.valueOf(attrs.getOrDefault("role", "USER"));

        if (!"ADMIN".equalsIgnoreCase(role)) {
            messaging.convertAndSend("/topic/chat.admin.errors",
                    new WsError("error", "Only role=ADMIN can send admin messages."));
            return;
        }

        MessageDto saved = chatService.sendAdminMessage(adminId, req.conversationId(), req.content());

        chatService.getConversationOwnerUserId(req.conversationId()).ifPresent(ownerUserId -> {
            messaging.convertAndSend("/topic/chat.user." + ownerUserId, saved);
        });

        messaging.convertAndSend("/topic/chat.admin.inbox", saved);
    }

    public record WsSend(@NotBlank String content) {}
    public record WsAdminSend(UUID conversationId, @NotBlank String content) {}
}
