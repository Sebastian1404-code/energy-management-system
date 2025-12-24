package distributedSystem.CustomerSupport.dto;


import distributedSystem.CustomerSupport.Domain.Role;
import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        String senderId,
        Role senderRole,
        String content,
        Instant createdAt
) {}
