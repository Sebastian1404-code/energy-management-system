package distributedSystem.CustomerSupport.web;


import distributedSystem.CustomerSupport.Domain.Role;
import distributedSystem.CustomerSupport.service.ChatService;
import distributedSystem.CustomerSupport.dto.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support")
public class ChatRestController {

    private final ChatService chatService;

    // USER: get or create conversation
    @GetMapping("/chat/conversation")
    public ConversationDto getMyConversation(@RequestHeader("X-User-Id") String userId) {
        return chatService.getOrCreateConversationForUser(userId);
    }

    // USER: send message
    @PostMapping("/chat/messages")
    public MessageDto userSend(@RequestHeader("X-User-Id") String userId,
                               @Valid @RequestBody SendMessageRequest req) {
        return chatService.sendUserMessage(userId, req.content());
    }

    // USER: message history
    @GetMapping("/chat/conversation/messages")
    public List<MessageDto> myMessages(@RequestHeader("X-User-Id") String userId) {
        ConversationDto conv = chatService.getOrCreateConversationForUser(userId);
        return chatService.getMessages(conv.id(), userId, Role.USER);
    }

    // ADMIN: list conversations (no auth here)
    @GetMapping("/admin/chat/conversations")
    @ResponseBody
    @CrossOrigin
    public List<ConversationDto> adminListConversations(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return chatService.listConversationsForAdmin();
    }
    // ADMIN: get messages
    @GetMapping("/admin/chat/conversations/{conversationId}/messages")
    public List<MessageDto> adminGetMessages(@RequestHeader(value = "X-Admin-Id", required = false) String adminId,
                                             @PathVariable UUID conversationId) {
        // adminId is optional here; kept for audit later
        return chatService.getMessages(conversationId, adminId == null ? "admin" : adminId, Role.ADMIN);
    }

    // ADMIN: send message
    @PostMapping("/admin/chat/conversations/{conversationId}/messages")
    public MessageDto adminSend(@RequestHeader(value = "X-Admin-Id", required = false) String adminId,
                                @PathVariable UUID conversationId,
                                @Valid @RequestBody SendMessageRequest req) {
        return chatService.sendAdminMessage(adminId == null ? "admin" : adminId, conversationId, req.content());
    }
}
