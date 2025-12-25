package distributedSystem.CustomerSupport.Domain;


import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Message {
    private UUID id;
    private UUID conversationId;
    private String senderId;
    private Role senderRole;
    private String content;
    private Instant createdAt;
}
