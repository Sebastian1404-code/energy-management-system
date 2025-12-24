package distributedSystem.CustomerSupport.dto;


import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        String userId,
        Instant createdAt,
        Instant updatedAt
) {}
